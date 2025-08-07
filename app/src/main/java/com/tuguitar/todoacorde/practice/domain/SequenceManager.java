package com.tuguitar.todoacorde.practice.domain;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.IChordDetector;
import com.tuguitar.todoacorde.LineItem;
import com.tuguitar.todoacorde.SpanInfo;
import com.tuguitar.todoacorde.practice.data.PracticeRepository;
import com.tuguitar.todoacorde.songs.data.SongChord;
import com.tuguitar.todoacorde.songs.data.SongLyric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SequenceManager {
    private static final String TAG = "SequenceManager";
    private final PracticeRepository repo;
    private final IChordDetector chordDetector;

    private final MediatorLiveData<Pair<List<Chord>, List<SongChord>>> seqSource = new MediatorLiveData<>();
    private final MediatorLiveData<List<LineItem>> lineItems = new MediatorLiveData<>();
    private LiveData<List<Chord>> chordProfiles;
    private LiveData<List<SongChord>> songChords;
    private LiveData<List<SpanInfo>> lyricLines; // asume exposición de las líricas por el repo

    @Inject
    public SequenceManager(PracticeRepository repo, IChordDetector chordDetector) {
        this.repo = repo;
        this.chordDetector = chordDetector;
    }

    /**
     * Debes llamar a initForSong(...) desde tu ViewModel una vez que tengas el songId.
     */
    public void initForSong(int songId) {
        // perfiles de acordes
        chordProfiles = repo.getAllChords();
        // acordes con tiempo/posiciones
        songChords   = repo.getChordsWithInfoForSong(songId);
        // líneas de letra (para el layout de LineItem)
        lyricLines   = repo.getLyricLinesForSong(songId);

        // combinar perfiles + acordes
        seqSource.addSource(chordProfiles, profs -> {
            chordDetector.setChordProfiles(profs);
            seqSource.setValue(Pair.create(profs, songChords.getValue()));
        });
        seqSource.addSource(songChords, sc -> {
            seqSource.setValue(Pair.create(chordProfiles.getValue(), sc));
        });

        // generar LineItem
        lineItems.addSource(seqSource, pair -> buildLineItems(pair.first, pair.second));
        lineItems.addSource(lyricLines, __ -> {
            // reconstruir cuando cambien también las líneas de letra
            buildLineItems(seqSource.getValue().first, seqSource.getValue().second);
        });
    }

    /** Secuencia de nombres de acordes lista para detectar. */
    public LiveData<List<Chord>> getChordProfiles() {
        return chordProfiles;
    }
    /** Secuencia de acordes con info (duraciones, posición). */
    public LiveData<List<SongChord>> getSongChords() {
        return songChords;
    }
    /** Pares [(perfiles, acordes)] combinados. */
    public LiveData<Pair<List<Chord>, List<SongChord>>> getSeqSource() {
        return seqSource;
    }
    /** Líneas renderizadas con acordes y texto. */
    public LiveData<List<LineItem>> getLineItems() {
        return lineItems;
    }

    private void buildSequenceAndIndexMap(List<Chord> profiles, List<SongChord> chords) {
        // ya se maneja dentro de buildLineItems si es necesario
    }

    private void buildLineItems(List<Chord> profiles, List<SongChord> scList) {
        if (profiles == null || scList == null || lyricLines.getValue() == null) {
            lineItems.setValue(Collections.emptyList());
            return;
        }

        // mapa id→nombre acorde
        Map<Integer, String> nameById = new HashMap<>();
        for (Chord c : profiles) nameById.put(c.id, c.getName());

        // ordenar acordes por letra
        List<SongChord> sorted = new ArrayList<>(scList);
        sorted.sort(Comparator
                .comparingInt((SongChord sc) -> sc.lyricId)
                .thenComparingInt(sc -> sc.positionInVerse)
        );

        List<LineItem> items = new ArrayList<>();
        for (SongLyric lyric : lyricLines.getValue()) {
            // construcción de buffer y spans
            String text = lyric.line != null ? lyric.line : "";
            int maxEnd = text.length();
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
}
