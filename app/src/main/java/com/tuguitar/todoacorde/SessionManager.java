package com.tuguitar.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Administra la sesión de usuario: almacena y provee el userId activo.
 * Por defecto, devuelve 1 si no se ha establecido ninguno.
 */
@Singleton
public class SessionManager {
    private static final String PREFS_NAME  = "session_prefs";
    private static final String KEY_USER_ID = "key_user_id";
    private final SharedPreferences prefs;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        prefs = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Devuelve el userId actualmente logueado,
     * o 1 si no se ha guardado ninguno todavía.
     */
    public long getCurrentUserId() {
        return prefs.getLong(KEY_USER_ID, 1L);
    }

    /**
     * Guarda el userId de la sesión actual.
     * (Llamarás a este método cuando implementes login en el futuro.)
     */
    public void setCurrentUserId(long userId) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    /**
     * Elimina la sesión (logout).
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .apply();
    }
}
