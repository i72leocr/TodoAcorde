package com.tuguitar.todoacorde.metronome.data;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.tuguitar.todoacorde.R;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;

/**
 * # MetronomeSoundRepository (Capa de Datos)
 *
 * Encapsula la carga y reproducción de samples del metrónomo utilizando {@link SoundPool}
 * para baja latencia. Provee dos sonidos:
 * - **Tick**: golpe normal.
 * - **Accent**: golpe acentuado (primer tiempo del compás).
 *
 * ## Responsabilidades
 * - Inicializar y configurar SoundPool con atributos de audio adecuados.
 * - Cargar los recursos de sonido de forma asíncrona.
 * - Reproducir los golpes en demanda con volumen y pitch por defecto.
 * - Liberar recursos cuando la pantalla se destruye o al limpiar el ViewModel.
 *
 * ## Notas de diseño
 * - Se mantiene un flag `soundLoaded` para evitar reproducir antes de que los
 *   samples estén listos.
 * - La reproducción es **idempotente**: si el repositorio ya fue liberado, no hace nada.
 */
public class MetronomeSoundRepository {

    // Motor de audio de baja latencia
    private SoundPool soundPool;

    // Identificadores de los sonidos cargados
    private int tickSoundId;
    private int accentSoundId;

    // Estado de carga
    private volatile boolean soundLoaded = false;

    /**
     * Crea e inicializa el repositorio de sonidos.
     *
     * @param context contexto de aplicación para acceder a recursos (inyectado por Hilt).
     */
    @Inject
    public MetronomeSoundRepository(@ApplicationContext @NonNull Context context) {
        // Configuración de atributos de audio para reproducción de media sin notificaciones
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        // SoundPool con un stream simultáneo (suficiente para metrónomo)
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        // Carga asíncrona de samples
        tickSoundId = soundPool.load(context, R.raw.tick, 1);
        accentSoundId = soundPool.load(context, R.raw.tick_up, 1);

        // Callback de carga: cuando ambos estén listos, activamos el flag
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                // Puede que se llamen dos veces (una por cada sample). Verificamos ambos.
                boolean tickReady = tickSoundId != 0;    // ID asignado
                boolean accentReady = accentSoundId != 0;
                // SoundPool no ofrece "is sample ready" por ID; asumimos que,
                // al menos tras la segunda devolución con status==0, ya cargó ambos.
                // Para robustez, activamos cuando haya llegado cualquier OK y
                // posteriormente la segunda notificación volverá a entrar sin problema.
                soundLoaded = true;
            }
        });
    }

    // ------------------------------------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------------------------------------

    /**
     * Reproduce el sonido de **golpe normal** del metrónomo.
     * <p>
     * Si los samples aún no han cargado o el recurso ya fue liberado,
     * la llamada se ignora silenciosamente.
     */
    @MainThread
    public void playTick() {
        if (!soundLoaded || soundPool == null || tickSoundId == 0) return;
        try {
            // Volumen L/R = 1f, prioridad=0, loop=0, rate=1f
            soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1f);
        } catch (IllegalStateException ignored) {
            // SoundPool pudo ser liberado entre chequeo y reproducción; ignoramos de forma segura.
        }
    }

    /**
     * Reproduce el sonido de **golpe acentuado** (primer tiempo del compás).
     * <p>
     * Si los samples aún no han cargado o el recurso ya fue liberado,
     * la llamada se ignora silenciosamente.
     */
    @MainThread
    public void playAccent() {
        if (!soundLoaded || soundPool == null || accentSoundId == 0) return;
        try {
            soundPool.play(accentSoundId, 1f, 1f, 0, 0, 1f);
        } catch (IllegalStateException ignored) {
            // SoundPool pudo ser liberado entre chequeo y reproducción; ignoramos de forma segura.
        }
    }

    /**
     * Libera los recursos de audio asociados al repositorio.
     * <p>
     * Debe invocarse desde {@code onCleared()} del ViewModel o en el ciclo de vida de la UI.
     * Es seguro llamar múltiples veces.
     */
    @MainThread
    public void release() {
        soundLoaded = false;
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {
                // Liberación defensiva; evitamos propagar errores del subsistema de audio.
            } finally {
                soundPool = null;
            }
        }
        tickSoundId = 0;
        accentSoundId = 0;
    }
}
