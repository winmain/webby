package webby.commons.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A handler for rejected tasks that adds the specified element to this queue,
 * waiting if necessary for space to become available.
 */
public class ForceQueuePolicy implements RejectedExecutionHandler {
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            executor.getQueue().put(r);
        } catch (InterruptedException e) {
            //should never happen since we never wait
            throw new RuntimeException(e);
        }
    }
}
