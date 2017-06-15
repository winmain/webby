package webby.form.field.upload

/**
  * Минимальное API, необходимое для работы [[UploadField]].
  * Предполагается, что экземпляр этого класса уже содержит в себе `token` и `suffix`.
  */
trait PreparedStorageServerApi {

  // ------------------------------- Config methods -------------------------------

  /** Базовый урл для загрузки файлов клиент-сервер и для получения информации об этой загрузке. Здесь запросы должны идти напрямую на сервер (минуя nginx) */
  def externalUploadBaseUrlHttp: String

  /** HTTPS-версия базового урла для загрузки файлов клиент-сервер. */
  def externalUploadBaseUrlHttps: String

  /** Базовый урл хостинга загруженных и временных файлов клиент-сервер. Здесь запросы должны идти через nginx. */
  def externalFileBaseUrl: String

  // ------------------------------- Api methods -------------------------------

  /**
    * Зарегистрировать новый токен и получить его id.
    */
  def newToken(): String

  /**
    * Сохранить временный файл и получить путь постоянного хранения.
    * Сохранение может вернуть None, если storage server вернул 404 (скорее всего потому что временный файл был удалён по времени).
    *
    * @param tmpName Временное имя файла, полученное при загрузке.
    */
  def store(tmpName: String): Option[StorageServerStoreResult]

  /**
    * Удалить файл из хранилища и вернуть количество реально удалённых файлов.
    *
    * @param name Имя файла, полученное от метода store()
    */
  def delete(name: String): Int

  /** Является ли хранимая запись временной? */
  def isTempFile(name: String): Boolean = name.startsWith("tmp/")

  // ------------------------------- Token methods -------------------------------

  // maps to token.strictlyJpg
  def acceptOnlyImage: Boolean
}

trait StorageServerStoreResult {
  def path: String
  def fileSize: Int
}
