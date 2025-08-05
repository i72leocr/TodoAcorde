package com.tuguitar.todoacorde;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Value object para normalizar y fijar un familyId consistente a partir de un título legible.
 * Ej: "Moderato Maestro" -> "moderato_maestro"
 */
public final class FamilyId {

    private final String value;

    private FamilyId(String raw) {
        this.value = normalize(raw);
    }

    public static FamilyId of(String rawTitle) {
        return new FamilyId(rawTitle);
    }

    public String asString() {
        return value;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{M}", "");
        return noAccents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // quitar caracteres raros
                .trim()
                .replaceAll("\\s+", "_"); // espacios a guiones bajos
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

    @Override
    public String toString() {
        return value;
    }
}
