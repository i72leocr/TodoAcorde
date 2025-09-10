package com.todoacorde.todoacorde;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Punto de entrada de la aplicación para Hilt.
 *
 * Al anotar la clase {@link Application} con {@link HiltAndroidApp} se
 * genera y configura automáticamente el grafo de dependencias a nivel
 * de aplicación. Debe declararse en el AndroidManifest como
 * <application android:name=".TodoAcordeApp" ... />
 */
@HiltAndroidApp
public class TodoAcordeApp extends Application {
}
