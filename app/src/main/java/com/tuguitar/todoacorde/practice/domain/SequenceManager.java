package com.tuguitar.todoacorde.practice.domain;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.IChordDetector;
import com.tuguitar.todoacorde.practice.data.LineItem;
import com.tuguitar.todoacorde.practice.data.SpanInfo;
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
    private LiveData<List<SongLyric>> lyricLines;

    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentLineIndex = new MutableLiveData<>(0);
    @Inject
    public SequenceManager(PracticeRepository repo, IChordDetector chordDetector) {
        this.repo = repo;
        this.chordDetector = chordDetector;
    }

    public void initForSong(int songId) {
        chordProfiles = repo.getAllChords();
        songChords = repo.getChordsForSong(songId);
        lyricLines = repo.getLyricsForSong(songId);
        seqSource.addSource(chordProfiles, profs -> {
            chordDetector.setChordProfiles(profs);
            List<SongChord> chords = songChords != null ? songChords.getValue() : null;
            if (chords != null) {
                seqSource.setValue(Pair.create(profs, chords));
            }
        });

        seqSource.addSource(songChords, sc -> {
            List<Chord> profs = chordProfiles != null ? chordProfiles.getValue() : null;
            if (profs != null) {
                seqSource.setValue(Pair.create(profs, sc));
            }
        });
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

    public LiveData<List<Chord>> getChordProfiles() {
        return chordProfiles;
    }

    public LiveData<List<SongChord>> getSongChords() {
        return songChords;
    }

    public LiveData<Pair<List<Chord>, List<SongChord>>> getSeqSource() {
        return seqSource;
    }

    public LiveData<List<LineItem>> getLineItems() {
        return lineItems;
    }

    private void buildLineItems(List<Chord> profiles, List<SongChord> scList) {
        if (profiles == null || scList == null || lyricLines.getValue() == null) {
            lineItems.setValue(Collections.emptyList());
            return;
        }

        Map<Integer, String> nameById = new HashMap<>();
        for (Chord c : profiles) nameById.put(c.id, c.getName());

        List<SongChord> sorted = new ArrayList<>(scList);
        sorted.sort(Comparator
                .comparingInt((SongChord sc) -> sc.lyricId)
                .thenComparingInt(sc -> sc.positionInVerse)
        );

        List<LineItem> items = new ArrayList<>();
        for (SongLyric lyric : lyricLines.getValue()) {
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
    public void reset() {
        currentIndex.setValue(0);
        currentLineIndex.setValue(0);
        lineItems.setValue(null);
    }
}
