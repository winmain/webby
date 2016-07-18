package webby.commons.system.log;

import java.io.IOException;

import ch.qos.logback.core.FileAppender;
import webby.commons.system.Signals;

/**
 * Пишет логи также как и {@link FileAppender}, но понимает unix сигнал (SIGUSR2) для переоткрытия
 * логов в стиле unix-way.
 *
 * @author den
 */
public class SignalRollingFileAppender<E> extends FileAppender<E> {

    /**
     * Отслеживаемый сигнал (USR2 по дефолту, потому что он не занят JVM).
     */
    private String signalName = "USR2";

    public String getSignalName() {
        return signalName;
    }

    public void setSignalName(String signalName) {
        if (signalName == null) {
            throw new IllegalArgumentException();
        }
        this.signalName = signalName;
    }

    @Override
    public synchronized void start() {
        super.start();

        Signals.install(signalName, s -> {
            synchronized (lock) {
                try {
                    // Закрыть старый файл
                    closeOutputStream();
                    // Переоткрыть новый файл
                    openFile(getFile());
                } catch (IOException e) {
                    addError("RolloverFailure occurred. Deferring roll-over.");
                    // we failed to roll-over, let us not truncate and risk data loss
                    append = true;
                }
            }
        });
    }
}
