package com.todoacorde.todoacorde.songs.domain;

/**
 * Conjunto de claves constantes para gestionar las preferencias de usuario
 * relacionadas con la visualización y ordenación de canciones.
 *
 * Se utilizan en {@code SharedPreferences} o equivalentes.
 */
public final class PrefsKeys {

    /** Constructor privado para evitar instanciación. */
    private PrefsKeys() {
    }

    /** Clave para mostrar únicamente canciones favoritas. */
    public static final String KEY_SHOW_FAV = "show_favorites_only";

    /** Clave que define el criterio de ordenación (título o dificultad). */
    public static final String KEY_SORT_CRIT = "sort_criterion";

    /** Clave que define el orden de clasificación (ascendente/descendente). */
    public static final String KEY_SORT_ASC = "sort_order";

    /** Valor de criterio: ordenar por título. */
    public static final String VAL_SORT_TITLE = "title";

    /** Valor de criterio: ordenar por dificultad. */
    public static final String VAL_SORT_DIFF = "diff";
}
