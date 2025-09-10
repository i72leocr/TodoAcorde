package com.todoacorde.todoacorde;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Identificador normalizado y estable para agrupar elementos por “familia”.
 *
 * Se genera a partir de una cadena arbitraria y aplica un proceso de
 * normalización para producir un valor en minúsculas, sin acentos,
 * sin signos de puntuación, con espacios colapsados y sustituidos por
 * guiones bajos. Esto facilita usarlo como clave en DB, mapas o rutas.</p>
 *
 * Ejemplo de normalización:
 *   "Mi Título Áéí óú!" → "mi_titulo_aei_ou"
 */
public final class FamilyId {

    /** Valor normalizado e inmutable del identificador. */
    private final String value;

    /**
     * Construye un {@code FamilyId} a partir del valor bruto ya normalizado.
     *
     * @param raw valor de entrada.
     */
    private FamilyId(String raw) {
        this.value = normalize(raw);
    }

    /**
     * Crea una instancia a partir de un título o etiqueta arbitraria.
     *
     * @param rawTitle texto base para generar el identificador.
     * @return instancia de {@code FamilyId}.
     */
    public static FamilyId of(String rawTitle) {
        return new FamilyId(rawTitle);
    }

    /**
     * Devuelve el identificador en forma de cadena.
     *
     * @return valor normalizado.
     */
    public String asString() {
        return value;
    }

    /**
     * Normaliza una cadena:
     * 1) descompone acentos, 2) elimina marcas diacríticas, 3) pasa a minúsculas,
     * 4) elimina caracteres no alfanuméricos ni espacios, 5) recorta extremos,
     * 6) colapsa múltiples espacios y 7) sustituye espacios por guiones bajos.
     *
     * @param input texto de entrada (puede ser {@code null}).
     * @return cadena normalizada apta como identificador.
     */
    private static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{M}", "");
        return noAccents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")   // solo alfanuméricos y espacios
                .trim()
                .replaceAll("\\s+", "_");         // espacios → guiones bajos
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FamilyId)) return false;
        FamilyId familyId = (FamilyId) o;
        return Objects.equals(value, familyId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Representación textual del identificador.
     *
     * @return el valor normalizado.
     */
    @Override
    public String toString() {
        return value;
    }
}
