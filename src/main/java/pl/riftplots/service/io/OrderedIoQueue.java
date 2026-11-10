package pl.riftplots.service.io;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Serializes background storage jobs; completion and failures are observable by callers.
 */
public final class OrderedIoQueue implements AutoCloseable {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "riftPlots-storage");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger pending = new AtomicInteger();
    private volatile String lastFailure = "";

    public int pending() {
        return pending.get();
    }

    public String lastFailure() {
        return lastFailure;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        pending.incrementAndGet();
        try {
            worker.execute(() -> {
                try {
                    result.complete(task.get());
                } catch (Throwable e) {
                    lastFailure = e.getClass().getSimpleName() + ": " + e.getMessage();
                    result.completeExceptionally(e);
                } finally {
                    pending.decrementAndGet();
                }
            });
        } catch (RejectedExecutionException e) {
            pending.decrementAndGet();
            result.completeExceptionally(e);
        }
        return result;
    }

    public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(pl.riftplots.config.Settings.longValue("performance.io-shutdown-timeout-seconds"), TimeUnit.SECONDS))
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.ordered-io-queue.storage-queue-did-not-finish-before-shutdown"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
