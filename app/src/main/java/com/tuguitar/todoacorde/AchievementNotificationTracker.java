package com.tuguitar.todoacorde;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tracker singleton que persiste en memoria qué logros ya se han notificado,
 * de modo que recreaciones de fragment/viewmodel no vuelvan a disparar la misma alerta.
 */
@Singleton
public class AchievementNotificationTracker {
    private final Set<String> notified = new HashSet<>();

    @Inject
    public AchievementNotificationTracker() { }

    public synchronized boolean wasNotified(String key) {
        return notified.contains(key);
    }

    public synchronized void markNotified(String key) {
        notified.add(key);
    }
}
