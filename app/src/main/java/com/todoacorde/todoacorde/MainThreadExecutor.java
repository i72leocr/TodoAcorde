package com.todoacorde.todoacorde;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Executor que despacha tareas en el hilo principal (UI thread) de Android.
 * Útil para publicar resultados en LiveData/Views tras ejecutar trabajo en background.
 */
public class MainThreadExecutor implements Executor {
    /** Handler asociado al Looper del hilo principal. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Encola la tarea para su ejecución en el hilo principal.
     *
     * @param command tarea a ejecutar.
     */
    @Override
    public void execute(Runnable command) {
        mainHandler.post(command);
    }
}
