package js.form.field.upload;

interface UploadFileType {
  /*
  Returns file type from filename (in common case it is a file extension)
   */
  function fromName(name: String): Null<String>;

  /*
  Returns image source for file type returned from method `fromName`
   */
  function getImageForType(tpe: String): Null<String>;
}
