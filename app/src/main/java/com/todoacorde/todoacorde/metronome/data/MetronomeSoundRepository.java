package com.todoacorde.todoacorde.metronome.data;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.todoacorde.todoacorde.R;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Repositorio responsable de cargar y reproducir los sonidos del metrónomo.
 *
 * Gestiona un {@link SoundPool} con dos muestras:
 * - Tick básico.
 * - Acento (primer tiempo del compás).
 *
 * Ciclo de vida:
 * - Las muestras se cargan asíncronamente al construir la clase.
 * - El flag {@code soundLoaded} indica disponibilidad para reproducción.
 * - Debe invocarse {@link #release()} cuando el componente ya no se use
 *   (por ejemplo, en {@code onDestroy} del fragmento/actividad) para liberar recursos nativos.
 */
public class MetronomeSoundRepository {

    /** Pool de reproducción de muestras de audio de baja latencia. */
    private SoundPool soundPool;

    /** Identificador de la muestra del tick básico dentro del pool. */
    private int tickSoundId;

    /** Identificador de la muestra acentuada dentro del pool. */
    private int accentSoundId;

    /** Indica si las muestras han terminado de cargarse y están listas. */
    private volatile boolean soundLoaded = false;

    /**
     * Inicializa el repositorio y carga las muestras del metrónomo.
     *
     * @param context contexto de aplicación inyectado por Hilt.
     */
    @Inject
    public MetronomeSoundRepository(@ApplicationContext @NonNull Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        tickSoundId = soundPool.load(context, R.raw.tick, 1);
        accentSoundId = soundPool.load(context, R.raw.tick_up, 1);

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                // Ambos IDs serán no cero cuando SoundPool haya registrado las muestras.
                // No es necesario comprobar cuál terminó: marcamos el flag global.
                soundLoaded = true;
            }
        });
    }

    /**
     * Reproduce el tick básico si las muestras están listas.
     * Debe llamarse en el hilo principal.
     */
    @MainThread
    public void playTick() {
        if (!soundLoaded || soundPool == null || tickSoundId == 0) return;
        try {
            soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1f);
        } catch (IllegalStateException ignored) {
            // Ignorar estados inválidos del pool (p. ej., si fue liberado entretanto).
        }
    }

    /**
     * Reproduce el acento del compás si las muestras están listas.
     * Debe llamarse en el hilo principal.
     */
    @MainThread
    public void playAccent() {
        if (!soundLoaded || soundPool == null || accentSoundId == 0) return;
        try {
            soundPool.play(accentSoundId, 1f, 1f, 0, 0, 1f);
        } catch (IllegalStateException ignored) {
            // Ignorar estados inválidos del pool (p. ej., si fue liberado entretanto).
        }
    }

    /**
     * Libera los recursos del {@link SoundPool} y reinicia el estado interno.
     * Debe llamarse cuando el repositorio ya no sea necesario para evitar fugas de recursos.
     * Debe llamarse en el hilo principal.
     */
    @MainThread
    public void release() {
        soundLoaded = false;
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {
                // Evitar caída de la app si el pool ya se encontraba en estado inválido.
            } finally {
                soundPool = null;
            }
        }
        tickSoundId = 0;
        accentSoundId = 0;
    }
}
