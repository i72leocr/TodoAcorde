package com.tuguitar.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Singleton;

/**
 * Módulo para proveer SharedPreferences de aplicación.
 */
@Module
@InstallIn(SingletonComponent.class)
public class PreferencesModule {

    private static final String PREFS_NAME = "SongPreferences";

    @Provides
    @Singleton
    public static SharedPreferences provideSharedPreferences(
            @ApplicationContext Context context
    ) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
