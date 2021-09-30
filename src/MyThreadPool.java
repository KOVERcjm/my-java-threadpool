import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadPool extends Thread {
    private final int n;                    // Initial thread count
    private int N;                          // Capacity of threads
    private AtomicInteger M;                // Current task count
    private BlockingQueue blockingQueue;    // Blocking queue for Callables

    /**
     * Initialization with initial thread count and capacity
     */
    public MyThreadPool(int N, int n) {
        this.N = Math.max(1, N);
        this.n = Math.max(0, n);
        M = new AtomicInteger(0);
        blockingQueue = new BlockingQueue(0);

        for (int i = 1; i <= n; i++) {
            Worker worker = new Worker(blockingQueue);
            M.getAndIncrement();
        }
    }

    /**
     * Homemade blocking queue storing Callables
     */
    class BlockingQueue {
        private final int limit;                            // Queue limitation for futher use
        private final LinkedList<Callable> callableQueue;   // Linked list storing the Callables
        public BlockingQueue(int limit) {
            this.limit = limit >= 0 ? limit : 0;            // 0 for unlimited
            callableQueue = new LinkedList<>();
        }

        /**
         * Add job to queue
         */
        public void schedule(Callable callable) {
            synchronized (callableQueue) {
                if (limit > 0 && callableQueue.size() > limit) {
                    // TODO Deny policy for over-limited runnable
                    return;
                } else {
                    callableQueue.addLast(callable);
                    callableQueue.notifyAll();
                }
            }
        }

        /**
         * Get job from queue
         */
        public Callable pop() throws InterruptedException {
            synchronized (callableQueue) {
                while (callableQueue.isEmpty()) {
                    try {
                        callableQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return callableQueue.removeFirst();
        }
    }

    /**
     * Homemade worker for processing jobs
     */
    class Worker implements Callable {
        private final BlockingQueue blockingQueue;      // Job source
        private volatile boolean isRunning;             // Worker status
        public Worker(BlockingQueue blockingQueue) {
            this.blockingQueue = blockingQueue;
            isRunning = true;
        }

        @Override
        public Object call() {
            while (isRunning && M.get() < N) {                          // Keep checking for jobs
                try {
                    Callable job = blockingQueue.pop();
                    if (job != null) {
                        FutureTask futureTask = new FutureTask(job);
                        futureTask.run();
                        M.getAndDecrement();
                        return futureTask.get();
                    }
                } catch (Exception e) {
                    isRunning = false;
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * Homemade thread factory
     */
    class MyThreadFactory implements ThreadFactory {
        private static final AtomicInteger count = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(null, r, "MyThread" + count.getAndIncrement());
        }
    }

    /**
     * Getter and setter
     */
    public int getCapacity() {
        return N;
    }
    public int getActiveCount(){
        return M.get();
    }
    public void resize(int N) {
        if (N < 1) {
            throw new Error("Invalid capacity");
        }

        if (N >= this.N) {
            this.N = N;
        }
        if (N < this.N) {
            blockingQueue.notifyAll();
        }
    }

    /**
     * Add new job to pool
     */
    public void schedule(Callable callable) {
        if (callable == null) {
            throw new Error("No callable been scheduled.");
        }
        blockingQueue.schedule(callable);
    }
}
