package com.todoacorde.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Módulo Hilt que expone una instancia única de {@link SharedPreferences}
 * para la aplicación.
 */
@Module
@InstallIn(SingletonComponent.class)
public class PreferencesModule {

    /** Nombre del archivo de preferencias compartidas. */
    private static final String PREFS_NAME = "SongPreferences";

    /**
     * Proveedor de {@link SharedPreferences} en ámbito de aplicación.
     *
     * @param context contexto de aplicación inyectado por Hilt.
     * @return instancia singleton de SharedPreferences.
     */
    @Provides
    @Singleton
    public static SharedPreferences provideSharedPreferences(
            @ApplicationContext Context context
    ) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
