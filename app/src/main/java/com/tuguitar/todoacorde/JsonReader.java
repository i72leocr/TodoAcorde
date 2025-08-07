package com.tuguitar.todoacorde;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tuguitar.todoacorde.songs.data.SongRepositoryModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class JsonReader {

    public static List<Chord> readChordsFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.chords_repo, new TypeToken<List<Chord>>() {}.getType());
    }

    public static List<Difficulty> readDifficultiesFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.difficulties_repo, new TypeToken<List<Difficulty>>() {}.getType());
    }

    public static List<ChordType> readChordTypesFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.types_repo, new TypeToken<List<ChordType>>() {}.getType());
    }

    public static List<SongRepositoryModel> readSongsFromRaw(Context context) {
        return readJsonFromRaw(context, R.raw.song_repo, new TypeToken<List<SongRepositoryModel>>() {}.getType());
    }

    private static <T> List<T> readJsonFromRaw(Context context, int rawResId, Type type) {
        Gson gson = new Gson();
        try (InputStream is = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            List<T> result = gson.fromJson(reader, type);
            Log.d("JsonReader", "✅ Archivo leído desde raw ID: " + rawResId + " | Elementos: " + (result != null ? result.size() : 0));
            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            Log.e("JsonReader", "❌ Error leyendo archivo raw ID: " + rawResId, e);
            return new ArrayList<>();
        }
    }
}