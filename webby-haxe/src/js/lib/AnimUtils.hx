package js.lib;

class AnimUtils {
  /*
  Делаем линейное изменение параметра `lastValue`, чтобы достичь значения `targetValue`.
  Дельта времени задана `timeStempMs` в миллисекундах.
  Скорость изменения параметра в секунду - `speed`.

  Возвращает новое анимаированное значение параметра.
   */
  public static function linearTween(lastValue: Float, targetValue: Float, timeStepMs: Float, speed: Float): Float {
    if (lastValue == targetValue) return targetValue;

    var animDelta = timeStepMs / 1000 * speed;
    var targetDelta = targetValue - lastValue;
    return
      if (Math.abs(targetDelta) < animDelta) {
        targetValue;
      } else {
        lastValue + animDelta * (targetDelta > 0 ? 1 : -1);
      };
  }
}
