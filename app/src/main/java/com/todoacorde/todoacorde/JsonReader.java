package com.todoacorde.todoacorde;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para leer recursos JSON desde la carpeta {@code res/raw} usando Gson.
 *
 * Métodos principales:
 * - {@link #readJsonFromRaw(Context, int, Type)} para leer arrays o listas tipadas.
 * - {@link #readObjectFromRaw(Context, int, Type)} para leer un objeto tipado.
 * - {@link #readRawAsString(Context, int)} para obtener el contenido del raw como String.
 *
 * También incluye atajos específicos para cargar datos de dominio del proyecto:
 * dificultades, tipos de acordes, acordes y modelo de canciones.
 */
public final class JsonReader {

    private static final String TAG = "JsonReader";

    private JsonReader() {
        // Clase de utilidades: no instanciable.
    }

    /**
     * Lee un JSON almacenado en un recurso raw y lo deserializa como lista usando el {@code Type} indicado.
     *
     * Ejemplo de uso:
     * <pre>
     * Type t = new TypeToken&lt;List&lt;MiDto&gt;&gt;(){}.getType();
     * List&lt;MiDto&gt; lista = JsonReader.readJsonFromRaw(context, R.raw.mi_json, t);
     * </pre>
     *
     * @param context  contexto para acceder a recursos.
     * @param rawResId id del recurso en {@code res/raw}.
     * @param type     tipo genérico destino (por ejemplo, {@code new TypeToken<List<Dto>>(){}.getType()}).
     * @param <T>      tipo de elemento de la lista.
     * @return lista deserializada o lista vacía si hay error o contenido nulo.
     */
    public static <T> List<T> readJsonFromRaw(Context context, int rawResId, Type type) {
        Gson gson = new Gson();
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            List<T> result = gson.fromJson(reader, type);
            Log.d(TAG, "Array leído desde raw ID: " + rawResId +
                    " | elementos=" + (result != null ? result.size() : 0));
            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error leyendo array en raw ID: " + rawResId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lee un JSON almacenado en un recurso raw y lo deserializa como objeto del {@code Type} indicado.
     *
     * @param context  contexto para acceder a recursos.
     * @param rawResId id del recurso en {@code res/raw}.
     * @param type     tipo destino (por ejemplo, {@code MiDto.class} o un {@code TypeToken} complejo).
     * @param <T>      tipo de retorno.
     * @return objeto deserializado o {@code null} si falla la lectura o el parseo.
     */
    public static <T> T readObjectFromRaw(Context context, int rawResId, Type type) {
        Gson gson = new Gson();
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            T result = gson.fromJson(reader, type);
            String typeDesc = (type != null) ? String.valueOf(type) : "unknown";
            Log.d(TAG, "Objeto leído desde raw ID: " + rawResId + " | tipo=" + typeDesc);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error leyendo objeto en raw ID: " + rawResId, e);
            return null;
        }
    }

    /**
     * Devuelve el contenido íntegro de un recurso raw como cadena.
     *
     * @param context  contexto para acceder a recursos.
     * @param rawResId id del recurso en {@code res/raw}.
     * @return cadena con el contenido o {@code null} si falla la lectura.
     */
    public static String readRawAsString(Context context, int rawResId) {
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString();
            Log.d(TAG, "String leído desde raw ID: " + rawResId + " | chars=" + json.length());
            return json;

        } catch (Exception e) {
            Log.e(TAG, "Error leyendo String en raw ID: " + rawResId, e);
            return null;
        }
    }

    /**
     * Atajo para leer la lista de canciones desde {@code R.raw.song_repo}.
     *
     * @param context contexto.
     * @return lista tipada de {@code SongRepositoryModel}.
     */
    public static List<com.todoacorde.todoacorde.songs.data.SongRepositoryModel> readSongsFromRaw(Context context) {
        return readJsonFromRaw(
                context,
                R.raw.song_repo,
                new TypeToken<List<com.todoacorde.todoacorde.songs.data.SongRepositoryModel>>() {}.getType()
        );
    }

    /**
     * Atajo para leer la lista de acordes desde {@code R.raw.chords_repo}.
     *
     * @param context contexto.
     * @return lista tipada de {@link Chord}.
     */
    public static List<Chord> readChordsFromRaw(Context context) {
        return readJsonFromRaw(
                context,
                R.raw.chords_repo,
                new TypeToken<List<Chord>>() {}.getType()
        );
    }

    /**
     * Atajo para leer el catálogo de dificultades desde {@code R.raw.difficulties_repo}.
     *
     * @param context contexto.
     * @return lista tipada de {@link Difficulty}.
     */
    public static List<Difficulty> readDifficultiesFromRaw(Context context) {
        return readJsonFromRaw(
                context,
                R.raw.difficulties_repo,
                new TypeToken<List<Difficulty>>() {}.getType()
        );
    }

    /**
     * Atajo para leer el catálogo de tipos de acorde desde {@code R.raw.types_repo}.
     *
     * @param context contexto.
     * @return lista tipada de {@link ChordType}.
     */
    public static List<ChordType> readChordTypesFromRaw(Context context) {
        return readJsonFromRaw(
                context,
                R.raw.types_repo,
                new TypeToken<List<ChordType>>() {}.getType()
        );
    }
}
