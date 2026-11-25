package com.blms.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * D5 Figure 3 «Pure Fabrication» BackgroundTaskRunner (GRASP Indirection): runs the controllers' long network jobs
 * (UC05 discovery, UC09 generation, UC11 dispatch) on a worker thread so the JavaFX thread never blocks.
 * The only class allowed to create threads.
 */
public class BackgroundTaskRunner {

    private final ExecutorService pool = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "blms-background");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong nextJobId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<Void>> jobs = new ConcurrentHashMap<>();

    /** SD05 step 5, SD09 step 2, SD11 step 2: returns the jobId reported in "...Started(jobId)". */
    public Long submit(Runnable task) {
        long jobId = nextJobId.getAndIncrement();
        jobs.put(jobId, CompletableFuture.runAsync(task, pool));
        return jobId;
    }

    /**
     * NOT IN D5 - approved addition (CONFLICTS.md gap G1): lets the UI learn that job jobId finished so it can
     * show the SSD replies discoveryCompleted / draftsReady / dispatchCompleted. Callbacks run on the WORKER
     * thread - the UI must wrap them in Platform.runLater (see ui/util/FxAsync).
     */
    public void onComplete(Long jobId, Runnable onSuccess, Consumer<Throwable> onError) {
        CompletableFuture<Void> job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job " + jobId);
        }
        job.whenComplete((ok, error) -> {
            jobs.remove(jobId);
            if (error == null) {
                onSuccess.run();
            } else {
                onError.accept(error.getCause() != null ? error.getCause() : error);
            }
        });
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
