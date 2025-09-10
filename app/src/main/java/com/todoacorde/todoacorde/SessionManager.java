package com.todoacorde.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Gestor de sesión simple basado en SharedPreferences.
 * Persistencia:
 * - Usa el archivo "session_prefs".
 * - Guarda el identificador del usuario activo bajo la clave "key_user_id".
 */
@Singleton
public class SessionManager {
    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_USER_ID = "key_user_id";

    private final SharedPreferences prefs;

    /**
     * Crea un SessionManager inyectando el contexto de aplicación.
     *
     * @param context contexto de aplicación provisto por Hilt.
     */
    @Inject
    public SessionManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Devuelve el identificador del usuario actualmente activo.
     *
     * @return id del usuario; por defecto 1L si no existe preferencia.
     */
    public long getCurrentUserId() {
        return prefs.getLong(KEY_USER_ID, 1L);
    }

    /**
     * Establece el identificador del usuario activo.
     *
     * @param userId id del usuario a persistir.
     */
    public void setCurrentUserId(long userId) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    /**
     * Limpia los datos de sesión eliminando el id de usuario almacenado.
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .apply();
    }
}
