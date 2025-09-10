package com.todoacorde.todoacorde.tuner.domain;

/**
 * Modelo inmutable que encapsula el resultado de la evaluación de afinación
 * de una cuerda o nota en el afinador.
 *
 * Contiene información suficiente para que la UI represente:
 * - El progreso relativo respecto al tono objetivo.
 * - Indicadores visuales y textuales sobre la acción necesaria (tensar/destensar).
 * - El desfase en Hz respecto a la frecuencia objetivo.
 */
public class TuningResult {

    /** Valor de progreso en rango 0–100 que indica la proximidad al tono correcto. */
    public final int progress;

    /** Marca si se debe mostrar un indicador de subir tono (destensar). */
    public final boolean showPlus;

    /** Marca si se debe mostrar un indicador de bajar tono (tensar). */
    public final boolean showMinus;

    /** Texto de acción asociado al resultado (por ejemplo "TENSAR" o "DESTENSAR"). */
    public final String actionText;

    /** Diferencia en Hz respecto a la frecuencia de referencia (puede ser positiva o negativa). */
    public final float offset;

    /**
     * Crea una nueva instancia de resultado de afinación.
     *
     * @param progress valor de progreso 0–100.
     * @param showPlus indicador de acción para subir tono.
     * @param showMinus indicador de acción para bajar tono.
     * @param actionText texto de instrucción asociado.
     * @param offset diferencia en Hz respecto a la referencia.
     */
    public TuningResult(int progress, boolean showPlus, boolean showMinus, String actionText, float offset) {
        this.progress = progress;
        this.showPlus = showPlus;
        this.showMinus = showMinus;
        this.actionText = actionText;
        this.offset = offset;
    }

    /**
     * Obtiene el desfase en Hz respecto a la frecuencia de referencia.
     *
     * @return valor float de desfase.
     */
    public float getOffset() {
        return offset;
    }
}
