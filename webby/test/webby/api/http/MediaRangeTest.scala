package webby.api.http

import org.scalatest.{Matchers, WordSpec}

class MediaRangeTest extends WordSpec with Matchers {

  "A MediaRange" should {
    "accept all media types" in {
      val mediaRange = MediaRange("*/*")
      mediaRange.accepts("text/html") shouldBe true
      mediaRange.accepts("application/json") shouldBe true
      mediaRange.accepts("foo/bar") shouldBe true
    }
    "accept a range of media types" in {
      val mediaRange = MediaRange("text/*")
      mediaRange.accepts("text/html") shouldBe true
      mediaRange.accepts("text/plain") shouldBe true
      mediaRange.accepts("application/json") shouldBe false
    }
    "accept a media type" in {
      val mediaRange = MediaRange("text/html")
      mediaRange.accepts("text/html") shouldBe true
      mediaRange.accepts("text/plain") shouldBe false
      mediaRange.accepts("application/json") shouldBe false
    }
  }
}