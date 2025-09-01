package com.tuguitar.todoacorde;

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
 * Utilidades para leer JSON desde res/raw con Gson.
 *
 * - readJsonFromRaw(...)     → listas (array raíz)  [compat con tu código actual]
 * - readObjectFromRaw(...)   → objetos (raíz objeto)
 * - readRawAsString(...)     → devuelve el JSON en String (para parsers manuales)
 */
public final class JsonReader {

    private static final String TAG = "JsonReader";

    private JsonReader() {}
    public static <T> List<T> readJsonFromRaw(Context context, int rawResId, Type type) {
        Gson gson = new Gson();
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            List<T> result = gson.fromJson(reader, type);
            Log.d(TAG, "✅ Array leído desde raw ID: " + rawResId +
                    " | Elementos: " + (result != null ? result.size() : 0));
            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error leyendo array en raw ID: " + rawResId, e);
            return new ArrayList<>();
        }
    }
    public static <T> T readObjectFromRaw(Context context, int rawResId, Type type) {
        Gson gson = new Gson();
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            T result = gson.fromJson(reader, type);
            String typeDesc = (type != null) ? String.valueOf(type) : "unknown";
            Log.d(TAG, "✅ Objeto leído desde raw ID: " + rawResId + " | Tipo: " + typeDesc);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error leyendo objeto en raw ID: " + rawResId, e);
            return null;
        }
    }
    public static String readRawAsString(Context context, int rawResId) {
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString();
            Log.d(TAG, "✅ String leído desde raw ID: " + rawResId + " | chars=" + json.length());
            return json;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error leyendo String en raw ID: " + rawResId, e);
            return null;
        }
    }

    public static List<com.tuguitar.todoacorde.songs.data.SongRepositoryModel> readSongsFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.song_repo,
                new TypeToken<List<com.tuguitar.todoacorde.songs.data.SongRepositoryModel>>() {}.getType());
    }

    public static List<Chord> readChordsFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.chords_repo, new TypeToken<List<Chord>>() {}.getType());
    }

    public static List<Difficulty> readDifficultiesFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.difficulties_repo, new TypeToken<List<Difficulty>>() {}.getType());
    }

    public static List<ChordType> readChordTypesFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.types_repo, new TypeToken<List<ChordType>>() {}.getType());
    }
}
