package js.lib;

class Keyboard {
  /*
  Производит замену латинских букв на русские, набранные по тем же клавишам.
   */
  public static function translitFromEnglish(text: String): String {
    var engLetters = "qwertyuiop[]asdfghjkl;'zxcvbnm,.";
    var rusLetters = "йцукенгшщзхъфывапролджэячсмитьбю";
    var ret = '';
    for (i in 0...text.length) {
      var c = text.charAt(i);
      var idx = engLetters.indexOf(c);
      if (idx != -1) ret += rusLetters.charAt(idx);
      else ret += c;
    }
    return ret;
  }
}
