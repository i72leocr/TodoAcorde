package com.todoacorde.todoacorde;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Proveedor singleton de pools de ejecución para operaciones comunes de la app.
 *
 * Responsabilidades:
 * - Ofrecer un {@link Executor} serial para operaciones de disco (diskIO).
 * - Ofrecer un {@link Executor} con varios hilos para trabajo de red (networkIO).
 * - Ofrecer un {@link Executor} que publica en el hilo principal (mainThread).
 *
 * Patrón:
 * - Implementación thread-safe con double-checked locking sobre un campo estático.
 * - Evita crear múltiples pools y centraliza la política de concurrencia.
 *
 * Notas:
 * - {@code MainThreadExecutor} debe existir en el proyecto y ejecutar tareas en el Looper principal.
 */
public class AppExecutors {

    /** Monitor para la inicialización perezosa del singleton. */
    private static final Object LOCK = new Object();

    /** Instancia única de {@link AppExecutors}. */
    private static AppExecutors instance;

    /** Ejecuta trabajo intensivo de E/S en un único hilo, preservando el orden. */
    private final Executor diskIO;

    /** Ejecuta trabajo concurrente (p. ej., red) en un pool de tamaño fijo. */
    private final Executor networkIO;

    /** Publica tareas en el hilo principal (UI). */
    private final Executor mainThread;

    /**
     * Crea una instancia con los {@link Executor} concretos.
     * Usada internamente por el patrón singleton.
     */
    private AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    /**
     * Devuelve la instancia global de {@link AppExecutors}.
     * Inicializa de forma perezosa con double-checked locking.
     */
    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AppExecutors(
                            // Un único hilo para operaciones de disco para evitar condiciones de carrera.
                            Executors.newSingleThreadExecutor(),
                            // Pool fijo para operaciones potencialmente bloqueantes (p. ej., HTTP).
                            Executors.newFixedThreadPool(3),
                            // Executor que despacha en el hilo principal de Android.
                            new MainThreadExecutor()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Executor serial para operaciones de disco (Room, ficheros, etc.).
     */
    public Executor diskIO() {
        return diskIO;
    }

    /**
     * Executor con pool fijo para trabajo concurrente (red/CPU moderado).
     */
    public Executor networkIO() {
        return networkIO;
    }

    /**
     * Executor que ejecuta en el hilo principal (UI).
     */
    public Executor mainThread() {
        return mainThread;
    }
}
