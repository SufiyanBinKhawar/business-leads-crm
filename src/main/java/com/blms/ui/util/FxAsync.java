package com.blms.ui.util;

import com.blms.app.AppContext;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * OWNER: Ismail. The ONLY bridge from BackgroundTaskRunner's worker threads back to the JavaFX thread
 * (UC05, UC09, UC11). Never touch a JavaFX node from a worker thread.
 */
public final class FxAsync {

    private FxAsync() {
    }

    /** Runs onSuccess / onError on the FX thread when job jobId (returned by a D5 controller) finishes. */
    public static void onComplete(Long jobId, Runnable onSuccess, Consumer<Throwable> onError) {
        AppContext.get().taskRunner().onComplete(jobId,
                () -> Platform.runLater(onSuccess),
                error -> Platform.runLater(() -> onError.accept(error)));
    }
}
