package com.tuguitar.todoacorde;
import androidx.room.TypeConverter;
/**
 * Conversor para que Room pueda persistir el enum Level.
 */
public final class Converters {

    private Converters() { /* no instanciable */ }

    @TypeConverter
    public static String fromLevel(Achievement.Level level) {
        return level.name();
    }

    @TypeConverter
    public static Achievement.Level toLevel(String name) {
        return Achievement.Level.valueOf(name);
    }
}
