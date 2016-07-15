package webby.commons.concurrent.executors;


import java.util.concurrent.*;

public class ScalingThreadPoolExecutor extends ThreadPoolExecutor {

    private volatile ShutdownListener listener;

    private final Object monitor = new Object();

    private final String name;

    public ScalingThreadPoolExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.name = name;
    }

    public void shutdown(ShutdownListener listener) {
        synchronized (monitor) {
            if (this.listener != null) {
                throw new IllegalStateException("Shutdown was already called on this thread pool");
            }
            if (isTerminated()) {
                listener.onTerminated();
            } else {
                this.listener = listener;
            }
        }
        shutdown();
    }

    @Override
    protected synchronized void terminated() {
        super.terminated();
        synchronized (monitor) {
            if (listener != null) {
                try {
                    listener.onTerminated();
                } finally {
                    listener = null;
                }
            }
        }
    }

    public interface ShutdownListener {
        void onTerminated();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ", " + super.toString() + ']';
    }
}
