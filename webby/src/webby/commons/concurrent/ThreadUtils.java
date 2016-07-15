package webby.commons.concurrent;

public class ThreadUtils {
    /**
     * Special method to avoid deprecation warnings calling thread.stop in Scala.
     */
    @SuppressWarnings("deprecation")
    static public void stop(Thread thread) {
        thread.stop();
    }
}
