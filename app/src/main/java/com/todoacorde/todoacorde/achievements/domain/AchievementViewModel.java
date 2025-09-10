package com.todoacorde.todoacorde.achievements.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementFamily;
import com.todoacorde.todoacorde.achievements.data.AchievementRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel encargado de orquestar la observación y agrupación de logros.
 *
 * - Observa el repositorio de logros y combina actualizaciones en familias {@link AchievementFamily}.
 * - Emite eventos de desbloqueo cuando un logro pasa a estado COMPLETED por primera vez.
 * - Evita notificaciones duplicadas utilizando {@link AchievementNotificationTracker}.
 */
@HiltViewModel
public class AchievementViewModel extends ViewModel {

    /** Lista observable de familias de logros para presentar en UI. */
    private final MediatorLiveData<List<AchievementFamily>> familiesLive = new MediatorLiveData<>();
    /** Evento de logros recién completados para mostrar notificaciones en UI. */
    private final MutableLiveData<Event<Achievement>> unlockEvent = new MutableLiveData<>();

    /** Última lista plana observada para detectar transiciones de estado. */
    private List<Achievement> previousFlat = Collections.emptyList();
    /** Indica si ya se recibió al menos una carga para evitar notificar en el primer render. */
    private boolean hasLoadedOnce = false;

    /** Tracker para evitar notificar el mismo logro más de una vez. */
    private final AchievementNotificationTracker notificationTracker;

    /** Descripciones por familia para enriquecer la presentación. */
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "Primeros Acorde", "Completa tu primera sesión de práctica.",
            "Moderato Maestro", "Desbloquea el modo moderado (speed 0.75x) en las canciones.",
            "Norma Legendaria", "Desbloquea el modo normal (speed 1x) en las canciones.",
            "Cien Por Ciento Rock", "Logra un score 100% en canciones en modo normal.",
            "Acorde Único", "Acierta acordes diferentes con éxito."
    );

    /**
     * Construye el ViewModel y establece las fuentes a observar.
     * Lanza una evaluación inicial de logros a través del repositorio.
     *
     * @param repository           repositorio de logros.
     * @param notificationTracker  componente para evitar notificaciones duplicadas.
     */
    @Inject
    public AchievementViewModel(
            AchievementRepository repository,
            AchievementNotificationTracker notificationTracker
    ) {
        this.notificationTracker = notificationTracker;
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
                    for (Achievement a : toNotify) {
                        unlockEvent.postValue(new Event<>(a));
                    }
                    if (!toNotify.isEmpty()) {
                        repository.evaluateAll();
                    }

                    previousFlat = new ArrayList<>(flatList);
                    familiesLive.postValue(groupIntoFamilies(flatList));
                    hasLoadedOnce = true;
                }
        );
    }

    /**
     * Devuelve la lista observable de familias de logros.
     *
     * @return LiveData con la lista de {@link AchievementFamily}.
     */
    public LiveData<List<AchievementFamily>> getAchievementFamilies() {
        return familiesLive;
    }

    /**
     * Devuelve el evento observable de logros recién desbloqueados.
     *
     * @return LiveData de eventos con {@link Achievement}.
     */
    public LiveData<Event<Achievement>> getUnlockEvent() {
        return unlockEvent;
    }

    /**
     * Agrupa una lista plana de logros en familias por título, ordenando por nivel.
     *
     * @param flat lista plana de logros.
     * @return lista de familias con sus niveles ordenados.
     */
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

    /**
     * Busca en una lista un logro que coincida por título y nivel.
     *
     * @param list   lista de candidatos.
     * @param target logro objetivo.
     * @return el logro coincidente si existe; de lo contrario null.
     */
    private Achievement findMatch(List<Achievement> list, Achievement target) {
        for (Achievement a : list) {
            if (a.getTitle().equals(target.getTitle())
                    && a.getLevel() == target.getLevel()) {
                return a;
            }
        }
        return null;
    }

    /**
     * Contenedor de evento de un solo uso para LiveData.
     *
     * @param <T> tipo del contenido del evento.
     */
    public static class Event<T> {
        private final T content;
        private boolean handled = false;

        /**
         * Crea un evento con el contenido proporcionado.
         *
         * @param content contenido asociado al evento.
         */
        public Event(T content) {
            this.content = content;
        }

        /**
         * Devuelve el contenido si aún no fue consumido; en caso contrario devuelve null.
         * Marca el evento como manejado tras la primera lectura.
         *
         * @return contenido no nulo si es la primera lectura; null si ya fue manejado.
         */
        public T getIfNotHandled() {
            if (handled) return null;
            handled = true;
            return content;
        }
    }
}
