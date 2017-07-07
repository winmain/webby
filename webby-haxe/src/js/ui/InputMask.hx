package js.ui;

import js.html.InputElement;
import Tag;

/*
  Задание маски для input-элемента на странице

  На данный момент поддерживает только '9' как подстановку для одной цифры
  и единственный '?' в маске как указание, что часть после вопроса - опциональная

  Например: '+7 999 999-99-99' для номера телефона или '99.99.9999' для даты
  Не позволяет ввести не цифры и автоматически форматирует ввод
 */
class InputMask {
  public var placeholderSymbol = '_';

  public var definitions: JMap<String, String -> Bool> = JMap.create();

  function isDigit(c: String): Bool return '0123456789'.indexOf(c) != -1;

  public function new() {
    definitions.set('9', isDigit);
  }

  /*
  Применить маску mask на заданном элементе tag.
   */
  public function applyOn(tag: Tag, mask: String) {
    var questionIndex: Int = mask.indexOf('?');
    var question: Bool = questionIndex != -1;
    if (question) mask = mask.replace('?', '');
    var len: Int = mask.length;
    var begin: String = mask;
    for (def in definitions.keys()) {
      begin = begin.substring(0, begin.indexOf(def));
    }
    var placeholder: String = question ? mask.substring(0, questionIndex) : mask;

    var minLen: Int = if (question) questionIndex else len;

    function isCorrect(text: String): Bool return
      text.length >= minLen &&
      text.length <= len &&
      text.indexOf(placeholderSymbol) == -1;

    for (def in definitions.keys()) {
      placeholder = placeholder.replace(new RegExp(def, 'g'), placeholderSymbol);
    }

    // TODO: сделать защиту от повторного вызова applyOn на том же элементе
    tag.on('input', function() {
      var original: String = tag.val();

      while (original.length > begin.length && original.charAt(original.length - 1) == placeholder.charAt(original.length - 1)) {
        original = original.substring(0, original.length - 1);
      }
      // original теперь содержит чистую строку, которую ввёл юзер, без placeholder'ов

      var inputEl: InputElement = cast tag.el;
      var caretPos: Int = inputEl.selectionStart;
      var text: String = original;
      // Не даём удалить кастомное начало маски
      if (text.length < begin.length) text = begin;

      for (index in 0...mask.length) {
        if (index >= text.length) break;
        var maskChar = mask.charAt(index);
        var checker = definitions.get(maskChar);
        if (checker != null) {
          // Удаляем все не-цифры, начиная с текущей позиции
          while (index < text.length && !checker(text.charAt(index))) {
            text = text.substring(0, index) + text.substring(index + 1);
          }
        }
        else if (text.charAt(index) != maskChar) {
          // Восстанавливаем форматирование из маски
          text = text.substring(0, index) + maskChar + text.substring(index);
          if (index <= caretPos) {
            caretPos++;
          }
        }
      }

      // Удаляем всё что не влезает в маску
      if (text.length > len) text = text.substring(0, len);
      text = text + placeholder.substring(text.length);
      tag.setVal(text);

      setCaretPosition(tag, G.toInt(Math.min(original.length, caretPos)));
    });

    // Вставляем в начало инпута начало маски при фокусе
    tag.on('focus', function() {
      if (tag.val() == "") tag.setVal(begin);
      else {
        // TODO: нужно вызвать select event у tag, чтобы выделить весь его текст
        // $el.select()
      }
    });

    // Если ввод не закончен - очистить поле
    tag.on('blur', function() {
      if (!isCorrect(tag.val())) tag.setVal(null);
    });
  }


  /*
  Устанавливает курсор в нужную позицию внутри инпута
  Бессовестно позаимствовано здесь: http://stackoverflow.com/a/512542
   */
  function setCaretPosition(tag: Tag, pos: Int) {
    var elem: InputElement = cast tag.el;
    if (untyped elem.createTextRange) {
      var range = untyped elem.createTextRange();
      range.move('character', pos);
      range.select();
    } else {
      if (untyped elem.selectionStart) {
        elem.focus();
        elem.setSelectionRange(pos, pos);
      } else {
        elem.focus();
      }
    }
  }
}
