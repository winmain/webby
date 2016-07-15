package webby.commons.system;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Безопасный способ подписывания обработчиков сигналов
 */
@SuppressWarnings("sunapi")
public class Signals implements SignalHandler {

    public static String USR = "USR";
    public static String USR2 = "USR2";

    // Static method to install the signal handler
    public static void install(String signalName, SigHandler handler) {
        Signal signal = new Signal(signalName);
        Signals safeSignalHandler = new Signals();
        SignalHandler oldHandler = Signal.handle(signal, safeSignalHandler);
        safeSignalHandler.setHandler(handler);
        safeSignalHandler.setOldHandler(oldHandler);
    }

    private SignalHandler oldHandler;
    private SigHandler handler;

    private Signals() {
    }

    private void setOldHandler(SignalHandler oldHandler) {
        this.oldHandler = oldHandler;
    }

    private void setHandler(SigHandler handler) {
        this.handler = handler;
    }

    // Signal handler method
    @Override
    public void handle(Signal sig) {
        try {
            handler.handle(sig.getName());
        } catch (Exception e) {
            System.err.println("Signal handler failed, reason " + e);
            e.printStackTrace();
        }

        // Chain back to previous handler, if one exists
        if (oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
            oldHandler.handle(sig);
        }
    }
}
