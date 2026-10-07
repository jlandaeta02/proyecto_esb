package com.bank.esb.concurrency;

import com.bank.esb.config.AppConfig;
import java.util.concurrent.*;

public class ConcurrencyManager {

    private final ExecutorService executor;

    public ConcurrencyManager() {
        AppConfig config = AppConfig.getInstance();

        int corePoolSize = config.getPoolCoreSize();
        int maxPoolSize = config.getPoolMaxSize();
        int queueCapacity = config.getPoolQueueCapacity();

        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);

        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public synchronized Thread newThread(Runnable r) {
                        counter++;
                        return new Thread(r, "ESB-Worker-Thread-" + counter);
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void execute(Runnable task) {
        this.executor.execute(task);
    }

    public void shutdown() {
        this.executor.shutdown();
    }
}
