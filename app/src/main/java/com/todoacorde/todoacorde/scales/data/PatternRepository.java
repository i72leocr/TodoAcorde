package com.todoacorde.todoacorde.scales.data;

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

/**
 * Repositorio para gestionar patrones de escalas y sus variantes en el diapasón.
 * Encapsula consultas a la base de datos y provee utilidades de normalización
 * y mapeo a modelos de dominio inmutables.
 *
 * Todas las operaciones anotadas con WorkerThread deben ejecutarse fuera del hilo principal.
 */
@Singleton
public class PatternRepository {

    /**
     * Modelo de dominio para un patrón de escala con sus notas.
     * Instancias son inmutables.
     */
    public static class ScalePattern {
        public final String name;
        public final String scaleType;
        public final String rootNote;
        public final List<ScaleFretNote> notes;

        /**
         * Crea un patrón de escala.
         *
         * @param name      nombre del patrón
         * @param scaleType tipo de escala (por ejemplo, Ionian, Dorian)
         * @param rootNote  nota raíz en texto
         * @param notes     lista de notas sobre el diapasón; si es null se usa lista vacía
         */
        public ScalePattern(String name, String scaleType, String rootNote, List<ScaleFretNote> notes) {
            this.name = name;
            this.scaleType = scaleType;
            this.rootNote = rootNote;
            this.notes = (notes != null) ? notes : new ArrayList<>();
        }
    }

    /**
     * Variante concreta de un patrón, acotada por ventana de trastes y posición.
     * Instancias son inmutables.
     */
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

        /**
         * Crea una variante de patrón.
         *
         * @param id            identificador del patrón
         * @param name          nombre descriptivo
         * @param scaleType     tipo de escala normalizado
         * @param rootNote      nota raíz normalizada
         * @param startFret     traste inicial de la ventana
         * @param endFret       traste final de la ventana
         * @param positionIndex índice de posición dentro del sistema
         * @param system        nombre del sistema o método (por ejemplo, CAGED)
         * @param notes         notas sobre el diapasón; si es null se usa lista vacía
         */
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

    /**
     * Inserta un patrón y sus notas en una transacción.
     *
     * @param pattern entidad patrón
     * @param notes   lista de entidades nota asociadas
     */
    @WorkerThread
    public void insertPatternWithNotes(@NonNull ScalePatternEntity pattern, @NonNull List<ScaleNoteEntity> notes) {
        dao.insertPatternWithNotes(pattern, notes);
    }

    /**
     * Borra todos los patrones y notas.
     */
    @WorkerThread
    public void clearAll() {
        dao.clearAllNotes();
        dao.clearAllPatterns();
    }

    /**
     * Devuelve el número de patrones almacenados.
     */
    @WorkerThread
    public int countPatterns() {
        return dao.countPatterns();
    }

    /**
     * Obtiene todos los patrones con sus notas mapeados a modelo de dominio.
     *
     * @return lista inmodificable de patrones
     */
    @WorkerThread
    @NonNull
    public List<ScalePattern> getAllPatterns() {
        List<PatternWithNotes> rows = dao.getAllPatternsWithNotes();
        return unmod(map(rows));
    }

    /**
     * Busca patrones por tipo de escala y raíz, aplicando normalización.
     *
     * @param scaleType tipo de escala, puede ser null
     * @param rootNote  nota raíz, puede ser null
     * @return lista inmodificable de patrones
     */
    @WorkerThread
    @NonNull
    public List<ScalePattern> findPatterns(@Nullable String scaleType, @Nullable String rootNote) {
        String st = normalizeType(trimOrNull(scaleType));
        String rn = normalizeRoot(rootNote);
        List<PatternWithNotes> rows = dao.findByScaleTypeAndRoot(st, rn);
        return unmod(map(rows));
    }

    /**
     * Devuelve todos los tipos de escala distintos normalizados.
     *
     * @return lista inmodificable de tipos de escala
     */
    @WorkerThread
    @NonNull
    public List<String> getAllScaleTypesDistinct() {
        List<String> types = dao.getAllScaleTypesDistinct();
        Set<String> canon = new LinkedHashSet<>();
        if (types != null) {
            for (String t : types) canon.add(normalizeType(t));
        }
        return unmod(new ArrayList<>(canon));
    }

    /**
     * Devuelve las raíces existentes para un tipo de escala concreto.
     *
     * @param scaleType tipo de escala, se normaliza
     * @return lista inmodificable de raíces disponibles, o vacía si no hay
     */
    @WorkerThread
    @NonNull
    public List<String> getRootsForType(@Nullable String scaleType) {
        String st = normalizeType(trimOrNull(scaleType));
        if (st == null || st.trim().isEmpty()) return Collections.emptyList();
        List<String> roots = dao.getRootsForType(st);
        return unmod(new ArrayList<>(roots != null ? roots : Collections.emptyList()));
    }

    /**
     * Obtiene variantes de patrón por tipo y raíz, aplicando normalización y
     * deduplicación por ventana de trastes y posición.
     *
     * @param scaleType tipo de escala, puede ser null
     * @param rootNote  raíz, puede ser null
     * @return lista inmodificable de variantes ordenadas por positionIndex, startFret e id
     */
    @WorkerThread
    @NonNull
    public List<PatternVariant> getVariantsByTypeAndRoot(@Nullable String scaleType, @Nullable String rootNote) {
        String st = normalizeType(trimOrNull(scaleType));
        String rn = normalizeRoot(rootNote);
        List<PatternWithNotes> rows = dao.findByScaleTypeAndRoot(st, rn);

        List<PatternVariant> tmp = new ArrayList<>();
        if (rows == null) return Collections.emptyList();

        for (PatternWithNotes r : rows) {
            if (r == null || r.pattern == null) continue;

            List<ScaleFretNote> notes = mapNotes(r.notes);
            int start = r.pattern.startFret;
            int end = r.pattern.endFret;

            // Si la ventana no está informada, se calcula a partir de las notas
            if (start == 0 && end == 0) {
                int[] win = computeWindow(notes);
                start = win[0];
                end = win[1];
            }

            tmp.add(new PatternVariant(
                    r.pattern.id,
                    r.pattern.name,
                    normalizeType(r.pattern.scaleType),
                    normalizeRoot(r.pattern.rootNote),
                    start,
                    end,
                    r.pattern.positionIndex,
                    r.pattern.system,
                    notes
            ));
        }

        // Deduplicación por clave compuesta, prefiriendo el id más alto
        Map<String, PatternVariant> pick = new LinkedHashMap<>();
        for (PatternVariant v : tmp) {
            String key = v.scaleType + "|" + v.rootNote + "|" + v.positionIndex + "|" + v.startFret + "|" + v.endFret;
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

    /**
     * Busca la variante cuya ventana esté más cercana a una ventana de preferencia.
     *
     * @param scaleType          tipo de escala
     * @param rootNote           raíz
     * @param preferredStartFret traste inicial preferido
     * @param preferredEndFret   traste final preferido
     * @return variante más próxima o null si no hay variantes
     */
    @WorkerThread
    @Nullable
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
            if (d < bestDist) {
                best = v;
                bestDist = d;
            }
        }
        return best;
    }

    /**
     * Mapea filas con notas a modelos de dominio ScalePattern.
     */
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

    /**
     * Mapea entidades de notas a modelo de dominio.
     */
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

    /**
     * Calcula la ventana mínima [minFret, maxFret] que contiene todas las notas.
     * Si no hay notas, devuelve [0, 12].
     */
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

    /**
     * Trim seguro que devuelve null si la cadena queda vacía.
     */
    @Nullable
    private static String trimOrNull(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Normaliza la nota raíz a notación con sostenidos cuando aplica.
     * Devuelve null si la entrada es null o vacía.
     */
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

    /**
     * Normaliza el tipo de escala a una forma canónica con mayúscula inicial
     * y mapeos específicos para alias conocidos.
     * Devuelve null si la entrada es null o vacía.
     */
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
            return k.substring(0, 1).toUpperCase(Locale.ROOT) + k.substring(1);
        }
        return k;
    }

    /**
     * Envuelve una lista como inmodificable.
     */
    @NonNull
    private static <T> List<T> unmod(@NonNull List<T> list) {
        return Collections.unmodifiableList(list);
    }
}
