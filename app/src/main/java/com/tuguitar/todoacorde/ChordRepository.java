package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.ChordDao;
import com.tuguitar.todoacorde.todoAcordeDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChordRepository {
    private final ChordDao chordDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public ChordRepository(ChordDao chordDao) {
        this.chordDao = chordDao;
    }

    /** Callback genérico para operaciones de lectura */
    public interface Callback<T> {
        void onResult(T result);
    }

    /** Inserta una lista de acordes (escritura en background) */
    public void insertAll(List<Chord> chords) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() ->
                chordDao.insertAll(chords)
        );
    }

    /** Inserta un solo acorde (escritura en background) */
    public void insert(Chord chord) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() ->
                chordDao.insert(chord)
        );
    }

    /** Lee todos los acordes y devuelve el resultado vía callback */
    /** Devuelve directamente el LiveData del DAO */
    public LiveData<List<Chord>> getAllChordsLive() {
        return chordDao.getAllChords();  // ahora LiveData<List<Chord>>
    }

    /** Lee un acorde por nombre y devuelve el resultado vía callback */
    public LiveData<Chord> getChordByNameLive(String name) {
        return chordDao.getChordByName(name);
    }


    /** Limpia el Executor cuando ya no sea necesario */
    public void shutdown() {
        io.shutdown();
    }
}
