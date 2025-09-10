package com.todoacorde.todoacorde.practice.domain;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.todoacorde.todoacorde.Chord;
import com.todoacorde.todoacorde.IChordDetector;
import com.todoacorde.todoacorde.practice.data.LineItem;
import com.todoacorde.todoacorde.practice.data.PracticeRepository;
import com.todoacorde.todoacorde.practice.data.SpanInfo;
import com.todoacorde.todoacorde.songs.data.SongChord;
import com.todoacorde.todoacorde.songs.data.SongLyric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Gestiona la secuencia de acordes y letras asociadas a una canción.
 * Se encarga de unir la información de acordes, perfiles y letras para
 * generar objetos {@link LineItem} que serán observados por la UI.
 */
public class SequenceManager {

    private static final String TAG = "SequenceManager";

    private final PracticeRepository repo;
    private final IChordDetector chordDetector;

    private final MediatorLiveData<Pair<List<Chord>, List<SongChord>>> seqSource = new MediatorLiveData<>();
    private final MediatorLiveData<List<LineItem>> lineItems = new MediatorLiveData<>();

    private LiveData<List<Chord>> chordProfiles;
    private LiveData<List<SongChord>> songChords;
    private LiveData<List<SongLyric>> lyricLines;

    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentLineIndex = new MutableLiveData<>(0);

    /**
     * Constructor con inyección de dependencias.
     *
     * @param repo          repositorio de práctica para acceder a datos
     * @param chordDetector detector de acordes para configurar perfiles
     */
    @Inject
    public SequenceManager(PracticeRepository repo, IChordDetector chordDetector) {
        this.repo = repo;
        this.chordDetector = chordDetector;
    }

    /**
     * Inicializa la secuencia para una canción específica.
     *
     * @param songId identificador de la canción
     */
    public void initForSong(int songId) {
        chordProfiles = repo.getAllChords();
        songChords = repo.getChordsForSong(songId);
        lyricLines = repo.getLyricsForSong(songId);

        /* Observa cambios en perfiles de acordes */
        seqSource.addSource(chordProfiles, profs -> {
            chordDetector.setChordProfiles(profs);
            List<SongChord> chords = songChords != null ? songChords.getValue() : null;
            if (chords != null) {
                seqSource.setValue(Pair.create(profs, chords));
            }
        });

        /* Observa cambios en acordes de la canción */
        seqSource.addSource(songChords, sc -> {
            List<Chord> profs = chordProfiles != null ? chordProfiles.getValue() : null;
            if (profs != null) {
                seqSource.setValue(Pair.create(profs, sc));
            }
        });

        /* Reconstruye las líneas cuando cambien los acordes o las letras */
        lineItems.addSource(seqSource, pair -> {
            if (pair != null) {
                buildLineItems(pair.first, pair.second);
            }
        });

        lineItems.addSource(lyricLines, __ -> {
            Pair<List<Chord>, List<SongChord>> pair = seqSource.getValue();
            if (pair != null) {
                buildLineItems(pair.first, pair.second);
            }
        });
    }

    /** @return perfiles de acordes */
    public LiveData<List<Chord>> getChordProfiles() {
        return chordProfiles;
    }

    /** @return acordes de la canción */
    public LiveData<List<SongChord>> getSongChords() {
        return songChords;
    }

    /** @return fuente combinada de perfiles y acordes */
    public LiveData<Pair<List<Chord>, List<SongChord>>> getSeqSource() {
        return seqSource;
    }

    /** @return elementos de línea con acordes y letras alineados */
    public LiveData<List<LineItem>> getLineItems() {
        return lineItems;
    }

    /**
     * Construye la lista de líneas (acordes + letras) a partir de los perfiles y acordes.
     */
    private void buildLineItems(List<Chord> profiles, List<SongChord> scList) {
        if (profiles == null || scList == null || lyricLines.getValue() == null) {
            lineItems.setValue(Collections.emptyList());
            return;
        }

        /* Mapa id->nombre para acordes */
        Map<Integer, String> nameById = new HashMap<>();
        for (Chord c : profiles) {
            nameById.put(c.id, c.getName());
        }

        /* Ordena los acordes por línea y posición */
        List<SongChord> sorted = new ArrayList<>(scList);
        sorted.sort(Comparator
                .comparingInt((SongChord sc) -> sc.lyricId)
                .thenComparingInt(sc -> sc.positionInVerse)
        );

        /* Construye línea por línea */
        List<LineItem> items = new ArrayList<>();
        for (SongLyric lyric : lyricLines.getValue()) {
            String text = lyric.line != null ? lyric.line : "";
            int maxEnd = text.length();

            /* Calcula el ancho necesario */
            for (SongChord sc : sorted) {
                if (sc.lyricId != lyric.id) continue;
                String nm = nameById.get(sc.chordId);
                if (nm != null) {
                    maxEnd = Math.max(maxEnd, sc.positionInVerse + nm.length());
                }
            }

            char[] buf = new char[maxEnd];
            Arrays.fill(buf, ' ');
            List<SpanInfo> spans = new ArrayList<>();

            /* Coloca cada acorde en la línea */
            for (int i = 0; i < sorted.size(); i++) {
                SongChord sc = sorted.get(i);
                if (sc.lyricId != lyric.id) continue;
                String nm = nameById.get(sc.chordId);
                int p = sc.positionInVerse;
                if (nm != null && p + nm.length() <= buf.length) {
                    for (int k = 0; k < nm.length(); k++) {
                        buf[p + k] = nm.charAt(k);
                    }
                    spans.add(new SpanInfo(i, p, p + nm.length()));
                }
            }

            items.add(new LineItem(new String(buf), text, spans));
        }

        lineItems.setValue(items);
        Log.d(TAG, "LineItems rebuild: " + items.size() + " lines");
    }

    /**
     * Reinicia el estado del gestor de secuencia.
     */
    public void reset() {
        currentIndex.setValue(0);
        currentLineIndex.setValue(0);
        lineItems.setValue(null);
    }
}
