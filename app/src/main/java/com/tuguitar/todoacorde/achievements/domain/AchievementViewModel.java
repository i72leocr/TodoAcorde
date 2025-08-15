package com.tuguitar.todoacorde.achievements.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.achievements.data.Achievement;
import com.tuguitar.todoacorde.achievements.data.AchievementFamily;
import com.tuguitar.todoacorde.achievements.data.AchievementRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AchievementViewModel extends ViewModel {

    private final MediatorLiveData<List<AchievementFamily>> familiesLive = new MediatorLiveData<>();
    private final MutableLiveData<Event<Achievement>> unlockEvent = new MutableLiveData<>();

    private List<Achievement> previousFlat = Collections.emptyList();
    private boolean hasLoadedOnce = false;

    private final AchievementNotificationTracker notificationTracker;

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "Primeros Acorde", "Completa tu primera sesión de práctica.",
            "Moderato Maestro", "Desbloquea el modo moderado (speed 0.75x) en las canciones.",
            "Norma Legendaria", "Desbloquea el modo normal (speed 1x) en las canciones.",
            "Cien Por Ciento Rock", "Logra un score 100% en canciones en modo normal.",
            "Acorde Único", "Acierta acordes diferentes con éxito."
    );

    @Inject
    public AchievementViewModel(
            AchievementRepository repository,
            AchievementNotificationTracker notificationTracker
    ) {
        this.notificationTracker = notificationTracker;

        // evaluación inicial
        repository.evaluateAll();

        familiesLive.addSource(
                repository.observeAll(),
                flatList -> {
                    if (flatList == null) return;

                    List<Achievement> toNotify = new ArrayList<>();

                    for (Achievement current : flatList) {
                        Achievement old = findMatch(previousFlat, current);
                        boolean justCompleted = current.getState() == Achievement.State.COMPLETED
                                && (old == null || old.getState() != Achievement.State.COMPLETED);

                        String key = current.getTitle() + "-" + current.getLevel();
                        if (justCompleted && hasLoadedOnce && !notificationTracker.wasNotified(key)) {
                            toNotify.add(current);
                            notificationTracker.markNotified(key);
                        }
                    }

                    // Emitir eventos para todos los completados detectados
                    for (Achievement a : toNotify) {
                        unlockEvent.postValue(new Event<>(a));
                    }

                    // Solo reevaluar si hubo al menos uno nuevo
                    if (!toNotify.isEmpty()) {
                        repository.evaluateAll();
                    }

                    previousFlat = new ArrayList<>(flatList);
                    familiesLive.postValue(groupIntoFamilies(flatList));
                    hasLoadedOnce = true;
                }
        );
    }

    public LiveData<List<AchievementFamily>> getAchievementFamilies() {
        return familiesLive;
    }

    public LiveData<Event<Achievement>> getUnlockEvent() {
        return unlockEvent;
    }

    private List<AchievementFamily> groupIntoFamilies(List<Achievement> flat) {
        Map<String, List<Achievement>> grouped = new LinkedHashMap<>();
        for (Achievement a : flat) {
            grouped
                    .computeIfAbsent(a.getTitle(), k -> new ArrayList<>())
                    .add(a);
        }

        List<AchievementFamily> out = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<Achievement>> entry : grouped.entrySet()) {
            String familyTitle = entry.getKey();
            String desc = DESCRIPTIONS.getOrDefault(familyTitle, "");
            List<Achievement> levels = entry.getValue();
            levels.sort(Comparator.comparing(Achievement::getLevel));
            out.add(new AchievementFamily(familyTitle, desc, levels));
        }
        return out;
    }

    private Achievement findMatch(List<Achievement> list, Achievement target) {
        for (Achievement a : list) {
            if (a.getTitle().equals(target.getTitle())
                    && a.getLevel() == target.getLevel()) {
                return a;
            }
        }
        return null;
    }

    public static class Event<T> {
        private final T content;
        private boolean handled = false;

        public Event(T content) {
            this.content = content;
        }

        public T getIfNotHandled() {
            if (handled) return null;
            handled = true;
            return content;
        }
    }
}
