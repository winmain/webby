package webby.commons.concurrent.executors;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class ExecutorScalingQueue<E> extends LinkedTransferQueue<E> {

    public ThreadPoolExecutor executor;

    public ExecutorScalingQueue() {
    }

    @Override
    public boolean offer(E e) {
        if (!tryTransfer(e)) {
            int left = executor.getMaximumPoolSize() - executor.getCorePoolSize();
            return left <= 0 && super.offer(e);
        } else {
            return true;
        }
    }
}
