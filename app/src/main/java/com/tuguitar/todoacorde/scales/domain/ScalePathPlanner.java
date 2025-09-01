package com.tuguitar.todoacorde.scales.domain;

import com.tuguitar.todoacorde.scales.data.NoteUtils;
import com.tuguitar.todoacorde.scales.data.ScaleFretNote;

import java.util.ArrayList;
import java.util.List;

/**
 * Vincula una secuencia de nombres de nota con posiciones reales en el patrón/caja.
 * Reglas clave:
 *  - Conserva el orden de expectedNotes.
 *  - Inicio: prioriza raíz; si hay varias, 6ª→1ª y menor traste.
 *  - Progresión: elige SIEMPRE el candidato con PITCH estrictamente mayor al anterior.
 *      * Empate de pitch: favorece cuerda más aguda (índice mayor, 6ª→1ª) y luego menor traste.
 *      * Si no existe pitch mayor (caso límite), cae a la opción más “cercana” evitando repetir posición.
 *  - Si una nota no tiene candidatos, repite la última posición válida para mantener la longitud.
 *
 * Notas:
 *  - stringIndex: 0=6ª, …, 5=1ª
 *  - Afinación estándar MIDI base por cuerda: E2(40), A2(45), D3(50), G3(55), B3(59), E4(64)
 */
public final class ScalePathPlanner {

    /** Orden de preferencia para ELEGIR la primera nota (tónica). 0=6ª … 5=1ª */
    private final int[] startPreferredStrings;

    /** MIDI base por cuerda (EADGBE). */
    private static final int[] STRING_BASE_MIDI = {40, 45, 50, 55, 59, 64};

    public ScalePathPlanner() {
        this(new int[]{0, 1, 2, 3, 4, 5});
    }

    public ScalePathPlanner(int[] startPreferredStrings) {
        this.startPreferredStrings = (startPreferredStrings != null && startPreferredStrings.length == 6)
                ? startPreferredStrings.clone()
                : new int[]{0, 1, 2, 3, 4, 5};
    }

    /**
     * @param expectedNotes nombres de nota (sin octava), ya normalizados por el VM a sostenidos
     * @param patternNotes  posiciones disponibles en la caja
     */
    public List<ScaleFretNote> bindSequenceToPattern(List<String> expectedNotes,
                                                     List<ScaleFretNote> patternNotes) {
        List<ScaleFretNote> path = new ArrayList<>();
        if (expectedNotes == null || expectedNotes.isEmpty() ||
                patternNotes == null || patternNotes.isEmpty()) {
            return path;
        }

        ScaleFretNote last = null;
        Integer lastPitch = null;

        for (String targetRaw : expectedNotes) {
            String target = NoteUtils.normalizeToSharp(targetRaw);
            List<ScaleFretNote> candidates = new ArrayList<>();
            for (ScaleFretNote n : patternNotes) {
                if (NoteUtils.equalsEnharmonic(n.noteName, target)) {
                    candidates.add(n);
                }
            }

            if (candidates.isEmpty()) {
                if (last != null) path.add(last);
                continue;
            }

            ScaleFretNote chosen;
            if (last == null) {
                chosen = chooseStartCandidate(candidates);
            } else {
                chosen = chooseNextAscending(last, lastPitch, candidates);
                if (chosen == null) {
                    chosen = chooseByManhattan(last, candidates);
                }
                if (chosen.stringIndex == last.stringIndex && chosen.fret == last.fret) {
                    ScaleFretNote alt = chooseAlternativeDifferentFrom(last, candidates);
                    if (alt != null) chosen = alt;
                }
            }

            path.add(chosen);
            last = chosen;
            lastPitch = pitchOf(chosen);
        }

        return path;
    }

    /** Primer punto: prioriza raíz (isRoot) y, si hay varias, preferencia de cuerda y luego traste bajo. */
    private ScaleFretNote chooseStartCandidate(List<ScaleFretNote> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;

        List<ScaleFretNote> pool = new ArrayList<>();
        for (ScaleFretNote c : candidates) if (c.isRoot) pool.add(c);
        if (pool.isEmpty()) pool = candidates;

        ScaleFretNote best = pool.get(0);
        int bestRank = rankOf(best);
        for (int i = 1; i < pool.size(); i++) {
            ScaleFretNote c = pool.get(i);
            int r = rankOf(c);
            if (r < bestRank) {
                best = c;
                bestRank = r;
            }
        }
        return best;
    }

    /**
     * Siguiente punto: intenta SIEMPRE avanzar a mayor pitch.
     * Empates de pitch: favorece cuerda más aguda (índice mayor) y menor traste.
     */
    private ScaleFretNote chooseNextAscending(ScaleFretNote last,
                                              Integer lastPitch,
                                              List<ScaleFretNote> candidates) {
        if (last == null || lastPitch == null || candidates == null || candidates.isEmpty()) {
            return null;
        }

        ScaleFretNote bestForward = null;
        int bestDelta = Integer.MAX_VALUE;
        int lastString = last.stringIndex;
        for (ScaleFretNote cand : candidates) {
            int p = pitchOf(cand);
            if (p > lastPitch) {
                int delta = p - lastPitch;
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestForward = cand;
                } else if (delta == bestDelta && bestForward != null) {
                    if (cand.stringIndex > bestForward.stringIndex ||
                            (cand.stringIndex == bestForward.stringIndex && cand.fret < bestForward.fret)) {
                        bestForward = cand;
                    }
                }
            }
        }
        if (bestForward != null) return bestForward;
        ScaleFretNote samePitch = null;
        for (ScaleFretNote cand : candidates) {
            int p = pitchOf(cand);
            if (p == lastPitch) {
                if (samePitch == null) samePitch = cand;
                else {
                    if (cand.stringIndex > samePitch.stringIndex ||
                            (cand.stringIndex == samePitch.stringIndex && cand.fret < samePitch.fret)) {
                        samePitch = cand;
                    }
                }
            }
        }
        if (samePitch != null &&
                !(samePitch.stringIndex == last.stringIndex && samePitch.fret == last.fret)) {
            return samePitch;
        }
        return chooseByManhattan(last, candidates);
    }

    /** Opción alternativa distinta a 'last' si existe. */
    private ScaleFretNote chooseAlternativeDifferentFrom(ScaleFretNote last, List<ScaleFretNote> candidates) {
        ScaleFretNote alt = null;
        int best = Integer.MAX_VALUE;
        for (ScaleFretNote c : candidates) {
            if (c.stringIndex == last.stringIndex && c.fret == last.fret) continue;
            int d = distanceManhattan(last, c);
            if (d < best) { best = d; alt = c; }
        }
        return alt;
    }

    /** Ranking de inicio: primero posición en startPreferredStrings, luego traste. */
    private int rankOf(ScaleFretNote n) {
        int stringRank = 100; // fallback grande
        for (int i = 0; i < startPreferredStrings.length; i++) {
            if (n.stringIndex == startPreferredStrings[i]) { stringRank = i; break; }
        }
        return stringRank * 100 + n.fret;
    }

    /** Distancia Manhattan con penalización de cambio de cuerda. */
    private int distanceManhattan(ScaleFretNote a, ScaleFretNote b) {
        return Math.abs(a.fret - b.fret) + Math.abs(a.stringIndex - b.stringIndex) * 2;
    }

    /** MIDI aproximado de una posición (EADGBE estándar). */
    private int pitchOf(ScaleFretNote n) {
        int base = (n.stringIndex >= 0 && n.stringIndex < STRING_BASE_MIDI.length)
                ? STRING_BASE_MIDI[n.stringIndex]
                : 40; // fallback E2
        return base + Math.max(0, n.fret);
    }

    /** Manhattan “puro” como último recurso. */
    private ScaleFretNote chooseByManhattan(ScaleFretNote last, List<ScaleFretNote> candidates) {
        ScaleFretNote best = candidates.get(0);
        int bestDist = distanceManhattan(last, best);
        for (int i = 1; i < candidates.size(); i++) {
            ScaleFretNote cand = candidates.get(i);
            int d = distanceManhattan(last, cand);
            if (d < bestDist) {
                best = cand; bestDist = d;
            } else if (d == bestDist) {
                if (cand.stringIndex > best.stringIndex ||
                        (cand.stringIndex == best.stringIndex && cand.fret < best.fret)) {
                    best = cand;
                }
            }
        }
        return best;
    }
}
