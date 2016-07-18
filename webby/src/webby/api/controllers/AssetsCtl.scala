package webby.api.controllers

import java.io._
import java.net.{JarURLConnection, URL}
import java.time.{Instant, LocalDateTime}
import java.util.Date

import com.google.common.io.ByteStreams
import com.google.common.net.HttpHeaders._
import io.netty.handler.codec.http.HttpResponseStatus
import org.apache.commons.codec.digest.DigestUtils
import webby.api._
import webby.api.libs._
import webby.api.mvc._
import webby.commons.system.log.PageLog
import webby.commons.time.StdDates
import webby.mvc.StdCtl

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * Controller that serves static resources.
 *
 * Resources are searched in the classpath.
 *
 * It handles Last-Modified and ETag header automatically.
 * If a gzipped version of a resource is found (Same resource name with the .gz suffix), it is served instead.
 *
 * You can set a custom Cache directive for a particular resource if needed. For example in your application.conf file:
 *
 * {{{
 * "assets.cache./public/images/logo.png" = "max-age=3600"
 * }}}
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.Assets.at(path="/public", file)
 * }}}
 */
object AssetsCtl extends AssetsCtlBuilder

class AssetsCtlBuilder extends StdCtl {

  private val timeZoneCode = "GMT"

  def df = StdDates.httpDateFormat

  private val parsableTimezoneCode = " " + timeZoneCode

  private val defaultCharSet = "utf-8"

  private def addCharsetIfNeeded(mimeType: String): String =
    if (MimeTypes.isText(mimeType))
      "; charset=" + defaultCharSet
    else ""

  /**
   * Generates an `Action` that serves a static resource.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`
   * @param file the file part extracted from the URL
   */
  def at(path: String, file: String): Action = SimpleAction { request =>
    PageLog.noLog()

    // -- LastModified handling
    def parseDate(date: String): Option[java.util.Date] = try {
      //jodatime does not parse timezones, so we handle that manually
      ////////val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).toDate
      val d = Date.from(Instant.from(df.parse(date)))
      Some(d)
    } catch {
      case NonFatal(_) => None
    }

    val resourceName = Option(path + "/" + file).map(name => if (name.startsWith("/")) name else "/" + name).get

    if (new File(resourceName).isDirectory || !new File(resourceName).getCanonicalPath.startsWith(new File(path).getCanonicalPath)) {
      NotFoundRaw
    } else {
      val app: Application = App.app
      val cleanResName = resourceName match {
        case s if s.startsWith("/") => s.drop(1)
        case s => s
      }
      val gzippedResource: Option[URL] = Option(app.classloader.getResource(cleanResName + ".gz"))

      val resource: Option[(URL, Boolean)] = {
        gzippedResource.map(_ -> true)
          .filter(_ => request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_ == "gzip" && app.profile.isProd)))
          .orElse(Option(app.classloader.getResource(cleanResName)).map(_ -> false))
      }

      resource.map {
        case (url, _) if new File(url.getFile).isDirectory => NotFoundRaw
        case (url, isGzipped) =>
          val bytes = ByteStreams.toByteArray(url.openStream())

          if (bytes.length == -1) {
            // TODO: если файл не найден, тот тут будет какая-то ошибка
            NotFoundRaw
          } else {
            request.headers.get(IF_NONE_MATCH).flatMap { ifNoneMatch =>
              etagFor(url).filter(_ == ifNoneMatch)
            }.map(_ => NotModified).getOrElse {
              request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).flatMap { ifModifiedSince =>
                lastModifiedFor(url).flatMap(parseDate).filterNot(lastModified => lastModified.after(ifModifiedSince))
              }.map(_ => NotModified.withHeader(DATE, df.format(LocalDateTime.now))).getOrElse {

                // Prepare a streamed response
                val response = new PlainResult(HttpResponseStatus.OK, bytes)
                  .withHeader(CONTENT_TYPE, MimeTypes.forFileName(file).map(m => m + addCharsetIfNeeded(m)).getOrElse(MimeTypes.BINARY))
                  .withHeader(DATE, df.format(LocalDateTime.now))

                // If there is a gzipped version, even if the client isn't accepting gzip, we need to specify the
                // Vary header so proxy servers will cache both the gzip and the non gzipped version
                val gzippedResponse = (gzippedResource.isDefined, isGzipped) match {
                  case (true, true) => response.withHeader(VARY, ACCEPT_ENCODING).withHeader(CONTENT_ENCODING, "gzip")
                  case (true, false) => response.withHeader(VARY, ACCEPT_ENCODING)
                  case _ => response
                }

                // Add Etag if we are able to compute it
                val taggedResponse = etagFor(url).map(etag => gzippedResponse.withHeader(ETAG, etag)).getOrElse(gzippedResponse)
                val lastModifiedResponse = lastModifiedFor(url).map(lastModified => taggedResponse.withHeader(LAST_MODIFIED, lastModified)).getOrElse(taggedResponse)

                // Add Cache directive if configured
                val cachedResponse = lastModifiedResponse.withHeader(CACHE_CONTROL, {
                  app.configuration.getString("\"assets.cache." + resourceName + "\"").getOrElse(app.profile match {
                    case Profile.Prod => app.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")
                    case _ => "no-cache"
                  })
                })
                cachedResponse
              }: Result
            }
          }
      }.getOrElse(NotFoundRaw)

    }

  }

  private val lastModifieds = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala

  private def lastModifiedFor(resource: java.net.URL): Option[String] = {
    lastModifieds.get(resource.toExternalForm).filter(_ => App.isProd).orElse {
      val maybeLastModified = resource.getProtocol match {
        case "file" => Some(df.format(StdDates.toInstant(new java.io.File(resource.getPath).lastModified)))
        case "jar" =>
          resource.getPath.split('!').drop(1).headOption.flatMap { fileNameInJar =>
            Option(resource.openConnection)
              .collect { case c: JarURLConnection => c }
              .flatMap(c => Option(c.getJarFile.getJarEntry(fileNameInJar.drop(1))))
              .map(_.getTime)
              .filterNot(_ == 0)
              .map(lastModified => df.format(StdDates.toInstant(lastModified)))
          }
        case _ => None
      }
      maybeLastModified.foreach(lastModifieds.put(resource.toExternalForm, _))
      maybeLastModified
    }
  }

  // -- ETags handling

  private val etags = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => App.isProd).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.toExternalForm).map("\"" + DigestUtils.md5Hex(_) + "\"")
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

}

