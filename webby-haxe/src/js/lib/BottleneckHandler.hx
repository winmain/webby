package js.lib;

/*
Handler, являющийся обёрткой над каким-нибудь действием, которое вызывается очень часто.
`BottleneckHandler` учитывает все входящие запросы, и вызывает `handler` не чаще чем раз в `delay` миллисекунд.
По сути, это ограничитель частоты запросов.

`handler` - это функциия, принимающая callback onFinish, который она должна вызвать по завершении.

Пример использования:

var bottleneck = new BottleneckHandler(function(onFinish) {
  // do something
  onFinish();
});

T.button.onClick(bottleneck.onHandle);

В таком случае, действие `do something` будет выполняться не чаще чем раз в 500 мс.
 */
class BottleneckHandler {
  private var startOver = false;
  private var timerStarted = false;

  private var handler: (Void -> Void) -> Void;
  private var delay: Int;

  public function new(handler: (Void -> Void) -> Void, delay: Int = 500) {
    this.handler = handler;
    this.delay = delay;
  }

  public function onHandle() {
    if (timerStarted) {
      startOver = true;
    } else {
      timerStarted = true;
      G.window.setTimeout(function() {
        startOver = false;
        handler(function() {
          timerStarted = false;
          if (startOver) onHandle();
        });
      }, delay);
    }
  }

  /*
  Упрощенная инициализация `BottleneckHandler` для синхронной функции. Упрощение состоит в том,
  что передаваемая функция не вызывает onFinish. Здесь onFinish вызывается сразу после функции `fn`.
   */
  public static function simple(fn: Void -> Void, delay: Int = 500): BottleneckHandler return
    new BottleneckHandler(function(onFinish) {
      fn();
      onFinish();
    }, delay);
}
