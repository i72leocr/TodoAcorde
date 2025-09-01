package com.tuguitar.todoacorde.scales.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PatternRepository {

    /** Modelo de dominio/UI para un patrón de escala. */
    public static class ScalePattern {
        public final String name;
        public final String scaleType;
        public final String rootNote;
        public final List<ScaleFretNote> notes;

        public ScalePattern(String name, String scaleType, String rootNote, List<ScaleFretNote> notes) {
            this.name = name;
            this.scaleType = scaleType;
            this.rootNote = rootNote;
            this.notes = (notes != null) ? notes : new ArrayList<>();
        }
    }

    /** Variante (caja) con ventana + metadatos. */
    public static class PatternVariant {
        public final long id;
        public final String name;
        public final String scaleType;
        public final String rootNote;
        public final int startFret;
        public final int endFret;
        public final int positionIndex;
        public final String system;
        public final List<ScaleFretNote> notes;

        public PatternVariant(long id, String name, String scaleType, String rootNote,
                              int startFret, int endFret, int positionIndex, String system,
                              List<ScaleFretNote> notes) {
            this.id = id;
            this.name = name;
            this.scaleType = scaleType;
            this.rootNote = rootNote;
            this.startFret = startFret;
            this.endFret = endFret;
            this.positionIndex = positionIndex;
            this.system = system;
            this.notes = (notes != null) ? notes : new ArrayList<>();
        }
    }

    private final ScalePatternDao dao;

    @Inject
    public PatternRepository(ScalePatternDao dao) {
        this.dao = dao;
    }

    @WorkerThread
    public void insertPatternWithNotes(@NonNull ScalePatternEntity pattern, @NonNull List<ScaleNoteEntity> notes) {
        dao.insertPatternWithNotes(pattern, notes);
    }

    @WorkerThread public void clearAll() { dao.clearAllNotes(); dao.clearAllPatterns(); }
    @WorkerThread public int countPatterns() { return dao.countPatterns(); }

    @WorkerThread @NonNull
    public List<ScalePattern> getAllPatterns() {
        List<PatternWithNotes> rows = dao.getAllPatternsWithNotes();
        return unmod(map(rows));
    }

    @WorkerThread @NonNull
    public List<ScalePattern> findPatterns(@Nullable String scaleType, @Nullable String rootNote) {
        String st = normalizeType(trimOrNull(scaleType));
        String rn = normalizeRoot(rootNote); // ⇦ bemoles→sostenidos
        List<PatternWithNotes> rows = dao.findByScaleTypeAndRoot(st, rn);
        return unmod(map(rows));
    }

    @WorkerThread @NonNull
    public List<String> getAllScaleTypesDistinct() {
        List<String> types = dao.getAllScaleTypesDistinct();
        Set<String> canon = new LinkedHashSet<>();
        if (types != null) {
            for (String t : types) canon.add(normalizeType(t));
        }
        return unmod(new ArrayList<>(canon));
    }

    @WorkerThread @NonNull
    public List<String> getRootsForType(@Nullable String scaleType) {
        String st = normalizeType(trimOrNull(scaleType));
        if (st == null || st.trim().isEmpty()) return Collections.emptyList();
        List<String> roots = dao.getRootsForType(st);
        return unmod(new ArrayList<>(roots != null ? roots : Collections.emptyList()));
    }

    /** Todas las variantes (cajas) para un tipo+raíz, sin duplicados “fantasma”. */
    @WorkerThread @NonNull
    public List<PatternVariant> getVariantsByTypeAndRoot(@Nullable String scaleType, @Nullable String rootNote) {
        String st = normalizeType(trimOrNull(scaleType));
        String rn = normalizeRoot(rootNote); // ⇦ soporta 'Bb' → 'A#'
        List<PatternWithNotes> rows = dao.findByScaleTypeAndRoot(st, rn);

        List<PatternVariant> tmp = new ArrayList<>();
        if (rows == null) return Collections.emptyList();

        for (PatternWithNotes r : rows) {
            if (r == null || r.pattern == null) continue;

            List<ScaleFretNote> notes = mapNotes(r.notes);
            int start = r.pattern.startFret;
            int end   = r.pattern.endFret;
            if (start == 0 && end == 0) {
                int[] win = computeWindow(notes);
                start = win[0]; end = win[1];
            }

            tmp.add(new PatternVariant(
                    r.pattern.id,
                    r.pattern.name,
                    normalizeType(r.pattern.scaleType),  // guardamos canónico en el modelo
                    normalizeRoot(r.pattern.rootNote),
                    start, end,
                    r.pattern.positionIndex,
                    r.pattern.system,
                    notes
            ));
        }
        Map<String, PatternVariant> pick = new LinkedHashMap<>();
        for (PatternVariant v : tmp) {
            String key = (v.scaleType + "|" + v.rootNote + "|" + v.positionIndex + "|" + v.startFret + "|" + v.endFret);
            PatternVariant prev = pick.get(key);
            if (prev == null || v.id > prev.id) pick.put(key, v);
        }
        List<PatternVariant> out = new ArrayList<>(pick.values());
        out.sort(Comparator
                .comparingInt((PatternVariant v) -> v.positionIndex)
                .thenComparingInt(v -> v.startFret)
                .thenComparingLong(v -> v.id));

        return unmod(out);
    }

    /** Variante más cercana a la ventana preferida (auto-pick). */
    @WorkerThread @Nullable
    public PatternVariant findNearestWindow(@Nullable String scaleType,
                                            @Nullable String rootNote,
                                            int preferredStartFret,
                                            int preferredEndFret) {
        List<PatternVariant> variants = getVariantsByTypeAndRoot(scaleType, rootNote);
        if (variants.isEmpty()) return null;

        int prefMid = (preferredStartFret + preferredEndFret) / 2;
        PatternVariant best = variants.get(0);
        int bestDist = Math.abs(((best.startFret + best.endFret) / 2) - prefMid);

        for (int i = 1; i < variants.size(); i++) {
            PatternVariant v = variants.get(i);
            int mid = (v.startFret + v.endFret) / 2;
            int d = Math.abs(mid - prefMid);
            if (d < bestDist) { best = v; bestDist = d; }
        }
        return best;
    }

    @NonNull
    private static List<ScalePattern> map(@Nullable List<PatternWithNotes> rows) {
        List<ScalePattern> out = new ArrayList<>();
        if (rows == null) return out;
        for (PatternWithNotes row : rows) {
            if (row == null || row.pattern == null) continue;
            List<ScaleFretNote> notes = mapNotes(row.notes);
            out.add(new ScalePattern(
                    row.pattern.name,
                    normalizeType(row.pattern.scaleType),
                    normalizeRoot(row.pattern.rootNote),
                    notes
            ));
        }
        return out;
    }

    @NonNull
    private static List<ScaleFretNote> mapNotes(@Nullable List<ScaleNoteEntity> notes) {
        List<ScaleFretNote> out = new ArrayList<>();
        if (notes == null) return out;
        for (ScaleNoteEntity n : notes) {
            if (n == null) continue;
            out.add(new ScaleFretNote(n.stringIndex, n.fret, n.degree, n.isRoot, n.tag, n.noteName));
        }
        return out;
    }

    @NonNull
    private static int[] computeWindow(@Nullable List<ScaleFretNote> notes) {
        if (notes == null || notes.isEmpty()) return new int[]{0, 12};
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (ScaleFretNote n : notes) {
            if (n == null) continue;
            if (n.fret < min) min = n.fret;
            if (n.fret > max) max = n.fret;
        }
        if (min == Integer.MAX_VALUE) min = 0;
        if (max == Integer.MIN_VALUE) max = 12;
        return new int[]{min, max};
    }

    @Nullable
    private static String trimOrNull(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Normaliza raíz: mayúsculas y bemoles → sostenidos (Db→C#, Eb→D#, Gb→F#, Ab→G#, Bb→A#). */
    @Nullable
    private static String normalizeRoot(@Nullable String root) {
        if (root == null) return null;
        String r = root.trim().toUpperCase(Locale.ROOT);
        if (r.isEmpty()) return null;
        switch (r) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
            case "E#": return "F";
            case "B#": return "C";
            default:   return r;
        }
    }

    /** Normaliza aliases/typos de tipos ingleses a forma canónica (coherente con VM/UseCases/Seeder). */
    @Nullable
    private static String normalizeType(@Nullable String st) {
        if (st == null) return null;
        String k = st.trim();
        if (k.isEmpty()) return null;

        String low = k.toLowerCase(Locale.ROOT);
        if (low.equals("ionion")) return "Ionian";
        if (low.equals("flamenco mode (major-phrygian)")
                || low.equals("flamenco mode")
                || low.equals("major-phrygian")
                || low.equals("major phrygian")
                || low.equals("spanish phrygian")
                || low.equals("phrygian dominant (flamenco)")) {
            return "Phrygian Dominant";
        }
        if (low.equals("pentatonic major")) return "Major Pentatonic";
        if (low.equals("pentatonic minor")) return "Minor Pentatonic";
        if (low.equals("spanish 8 tone")) return "Spanish 8-Tone";
        if (low.equals("melodic minor (asc)") || low.equals("melodic minor asc") || low.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }
        if (Character.isLowerCase(k.charAt(0))) {
            return k.substring(0,1).toUpperCase(Locale.ROOT) + k.substring(1);
        }
        return k;
    }
    @NonNull
    private static <T> List<T> unmod(@NonNull List<T> list) {
        return Collections.unmodifiableList(list);
    }
}
