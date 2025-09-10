package com.todoacorde.todoacorde;

import androidx.room.TypeConverter;

import com.todoacorde.todoacorde.achievements.data.Achievement;

/**
 * Clase de conversores de tipos personalizados para Room.
 * Permite transformar valores de {@link Achievement.Level} a {@link String} y viceversa,
 * de forma que puedan almacenarse en la base de datos sin pérdida de información.
 */
public final class Converters {

    /**
     * Constructor privado para evitar la instanciación de esta clase utilitaria.
     */
    private Converters() { }

    /**
     * Convierte un valor enumerado de {@link Achievement.Level} en su representación en texto.
     *
     * @param level instancia del enumerado {@link Achievement.Level}.
     * @return nombre del nivel como cadena de texto.
     */
    @TypeConverter
    public static String fromLevel(Achievement.Level level) {
        return level.name();
    }

    /**
     * Convierte un texto almacenado en la base de datos en un valor enumerado {@link Achievement.Level}.
     *
     * @param name cadena de texto que representa el nombre del nivel.
     * @return instancia del enumerado {@link Achievement.Level}.
     */
    @TypeConverter
    public static Achievement.Level toLevel(String name) {
        return Achievement.Level.valueOf(name);
    }
}
