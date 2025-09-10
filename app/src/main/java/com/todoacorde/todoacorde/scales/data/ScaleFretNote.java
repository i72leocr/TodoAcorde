package com.todoacorde.todoacorde.scales.data;

/**
 * Modelo inmutable que representa una nota ubicada en el diapasón para una escala.
 *
 * Contiene información suficiente para dibujar o analizar posiciones:
 * - Índice de cuerda (0 = sexta/grave, 5 = primera/aguda).
 * - Traste donde se ejecuta la nota.
 * - Grado relativo dentro de la escala.
 * - Indicador de si es la tónica.
 * - Dedo sugerido.
 * - Nombre de la nota.
 */
public class ScaleFretNote {

    /** Índice de la cuerda (0 = 6ª, 5 = 1ª). */
    public final int stringIndex;

    /** Número de traste (0 = al aire). */
    public final int fret;

    /** Grado relativo de la escala (por ejemplo: "1", "b3", "5"). */
    public final String degree;

    /** Indica si esta nota es la tónica de la escala. */
    public final boolean isRoot;

    /** Dedo sugerido para ejecutar la nota (puede estar vacío). */
    public final String finger;

    /** Nombre de la nota (por ejemplo: "C", "F#", "Bb"). */
    public final String noteName;

    /**
     * Crea una instancia inmutable de una nota en el diapasón.
     *
     * @param stringIndex índice de la cuerda (0 a 5)
     * @param fret traste donde se toca (>= 0)
     * @param degree grado relativo dentro de la escala
     * @param isRoot true si es la tónica
     * @param finger dedo sugerido (opcional, puede ser "")
     * @param noteName nombre de la nota musical
     */
    public ScaleFretNote(int stringIndex,
                         int fret,
                         String degree,
                         boolean isRoot,
                         String finger,
                         String noteName) {
        this.stringIndex = stringIndex;
        this.fret = fret;
        this.degree = degree;
        this.isRoot = isRoot;
        this.finger = finger;
        this.noteName = noteName;
    }
}
