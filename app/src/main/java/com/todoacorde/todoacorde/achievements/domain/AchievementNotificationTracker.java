package com.todoacorde.todoacorde.achievements.domain;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Registro en memoria de notificaciones de logros mostradas al usuario.
 *
 * Mantiene un conjunto de claves ya notificadas para evitar duplicidades
 * (por ejemplo, no repetir un toast o diálogo por el mismo logro).
 *
 * Características:
 * - Almacenamiento en memoria durante el ciclo de vida del proceso.
 * - Anotado como {@link Singleton} para compartir la misma instancia.
 * - Métodos sincronizados para garantizar seguridad frente a accesos concurrentes.
 */
@Singleton
public class AchievementNotificationTracker {

    /** Conjunto de claves de logros que ya han sido notificados. */
    private final Set<String> notified = new HashSet<>();

    /**
     * Constructor por inyección. No requiere dependencias externas.
     */
    @Inject
    public AchievementNotificationTracker() {
    }

    /**
     * Indica si una clave de logro ya fue notificada.
     *
     * @param key clave única del logro (por ejemplo, combinación familia+nivel).
     * @return {@code true} si la clave ya está registrada como notificada; en caso contrario {@code false}.
     */
    public synchronized boolean wasNotified(String key) {
        return notified.contains(key);
    }

    /**
     * Marca una clave de logro como notificada para evitar futuras repeticiones.
     *
     * @param key clave única del logro a registrar.
     */
    public synchronized void markNotified(String key) {
        notified.add(key);
    }
}
