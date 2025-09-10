package com.todoacorde.todoacorde.scales.domain;

import com.todoacorde.todoacorde.scales.data.NoteUtils;
import com.todoacorde.todoacorde.scales.data.ScaleFretNote;

import java.util.ArrayList;
import java.util.List;

/**
 * Planificador de ruta sobre un patrón de escala en el diapasón.
 *
 * Dado:
 * - Una secuencia de notas esperadas (como nombres de nota).
 * - Un patrón (lista de {@link ScaleFretNote}) que contiene posiciones válidas en el mástil.
 *
 * Devuelve:
 * - Una lista de posiciones del patrón que “sigue” la secuencia esperada
 *   priorizando movimiento ascendente y desplazamientos mínimos.
 *
 * Notas:
 * - Comentarios 100% en formato estándar de Android Studio/Java (sin HTML).
 */
public final class ScalePathPlanner {

    /* Prioridad de cuerdas para elegir el primer punto (índices 0..5). */
    private final int[] startPreferredStrings;

    /* MIDI base de cada cuerda al aire: E2, A2, D3, G3, B3, E4. */
    private static final int[] STRING_BASE_MIDI = {40, 45, 50, 55, 59, 64};

    /**
     * Constructor por defecto. Prioriza cuerdas 0..5 en orden.
     */
    public ScalePathPlanner() {
        this(new int[]{0, 1, 2, 3, 4, 5});
    }

    /**
     * Constructor con prioridad de cuerdas personalizada.
     *
     * @param startPreferredStrings arreglo de longitud 6 con el orden preferido de cuerdas
     */
    public ScalePathPlanner(int[] startPreferredStrings) {
        this.startPreferredStrings = (startPreferredStrings != null && startPreferredStrings.length == 6)
                ? startPreferredStrings.clone()
                : new int[]{0, 1, 2, 3, 4, 5};
    }

    /**
     * Enlaza una secuencia de notas esperadas con posiciones concretas del patrón.
     *
     * Estrategia:
     * 1) Para la primera nota, prioriza raíces y la cuerda/frete mejor rankeados.
     * 2) Para siguientes notas, intenta avanzar en altura (pitch) con el menor salto.
     * 3) En empate o imposibilidad de avanzar, minimiza distancia Manhattan (fret + diferencia de cuerda ponderada).
     * 4) Evita repetir exactamente la misma posición si existe una alternativa válida.
     *
     * @param expectedNotes lista de nombres de nota esperados (p. ej. "C", "D#", "E"...)
     * @param patternNotes  posiciones disponibles del patrón
     * @return lista de {@link ScaleFretNote} que sigue la secuencia
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

            /* Candidatos en el patrón que coinciden enharmónicamente con la nota objetivo. */
            List<ScaleFretNote> candidates = new ArrayList<>();
            for (ScaleFretNote n : patternNotes) {
                if (NoteUtils.equalsEnharmonic(n.noteName, target)) {
                    candidates.add(n);
                }
            }

            /* Si no hay candidatos, repite la última posición (si existe). */
            if (candidates.isEmpty()) {
                if (last != null) path.add(last);
                continue;
            }

            ScaleFretNote chosen;
            if (last == null) {
                /* Primera nota: elige mejor inicio. */
                chosen = chooseStartCandidate(candidates);
            } else {
                /* Intenta movimiento ascendente con el menor delta de pitch. */
                chosen = chooseNextAscending(last, lastPitch, candidates);

                /* Si no se pudo, minimiza distancia Manhattan. */
                if (chosen == null) {
                    chosen = chooseByManhattan(last, candidates);
                }

                /* Evita repetir exactamente la misma posición si hay alternativa. */
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

    /**
     * Selección de inicio: prioriza notas raíz; en empate, usa ranking por preferencia de cuerda y traste bajo.
     */
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
     * Intenta elegir el siguiente candidato con pitch mayor al último y el menor incremento posible.
     * En empate, prefiere cuerdas más agudas (índice mayor) y menor traste.
     * Si no hay avance, intenta mantener el mismo pitch con el mismo criterio.
     * Si tampoco, delega en Manhattan.
     */
    private ScaleFretNote chooseNextAscending(ScaleFretNote last,
                                              Integer lastPitch,
                                              List<ScaleFretNote> candidates) {
        if (last == null || lastPitch == null || candidates == null || candidates.isEmpty()) {
            return null;
        }

        ScaleFretNote bestForward = null;
        int bestDelta = Integer.MAX_VALUE;

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

        /* Intento con mismo pitch. */
        ScaleFretNote samePitch = null;
        for (ScaleFretNote cand : candidates) {
            int p = pitchOf(cand);
            if (p == lastPitch) {
                if (samePitch == null) samePitch = cand;
                else if (cand.stringIndex > samePitch.stringIndex ||
                        (cand.stringIndex == samePitch.stringIndex && cand.fret < samePitch.fret)) {
                    samePitch = cand;
                }
            }
        }
        if (samePitch != null &&
                !(samePitch.stringIndex == last.stringIndex && samePitch.fret == last.fret)) {
            return samePitch;
        }

        /* Fallback. */
        return chooseByManhattan(last, candidates);
    }

    /**
     * Busca una alternativa distinta a la última posición, minimizando distancia Manhattan.
     */
    private ScaleFretNote chooseAlternativeDifferentFrom(ScaleFretNote last, List<ScaleFretNote> candidates) {
        ScaleFretNote alt = null;
        int best = Integer.MAX_VALUE;
        for (ScaleFretNote c : candidates) {
            if (c.stringIndex == last.stringIndex && c.fret == last.fret) continue;
            int d = distanceManhattan(last, c);
            if (d < best) {
                best = d;
                alt = c;
            }
        }
        return alt;
    }

    /**
     * Ranking para primer punto: por orden de preferencia de cuerda y traste bajo.
     */
    private int rankOf(ScaleFretNote n) {
        int stringRank = 100;
        for (int i = 0; i < startPreferredStrings.length; i++) {
            if (n.stringIndex == startPreferredStrings[i]) {
                stringRank = i;
                break;
            }
        }
        return stringRank * 100 + n.fret;
    }

    /**
     * Distancia Manhattan con ponderación simple para cambio de cuerda.
     */
    private int distanceManhattan(ScaleFretNote a, ScaleFretNote b) {
        return Math.abs(a.fret - b.fret) + Math.abs(a.stringIndex - b.stringIndex) * 2;
    }

    /**
     * Calcula el pitch MIDI absoluto de una posición (cuerda base + traste).
     */
    private int pitchOf(ScaleFretNote n) {
        int base = (n.stringIndex >= 0 && n.stringIndex < STRING_BASE_MIDI.length)
                ? STRING_BASE_MIDI[n.stringIndex]
                : 40;
        return base + Math.max(0, n.fret);
    }

    /**
     * Selección por distancia Manhattan mínima. En empate, prefiere cuerda más aguda y traste menor.
     */
    private ScaleFretNote chooseByManhattan(ScaleFretNote last, List<ScaleFretNote> candidates) {
        ScaleFretNote best = candidates.get(0);
        int bestDist = distanceManhattan(last, best);
        for (int i = 1; i < candidates.size(); i++) {
            ScaleFretNote cand = candidates.get(i);
            int d = distanceManhattan(last, cand);
            if (d < bestDist) {
                best = cand;
                bestDist = d;
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
