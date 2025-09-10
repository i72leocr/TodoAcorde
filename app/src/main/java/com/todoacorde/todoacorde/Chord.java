package com.todoacorde.todoacorde;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Entidad Room que representa un acorde almacenado en la base de datos.
 *
 * Relaciones:
 * - {@code type_id} referencia a {@link ChordType#id} (con borrado en cascada).
 * - {@code difficulty_id} referencia a {@link Difficulty#id} (con borrado en cascada).
 *
 * Índices:
 * - Índice por {@code type_id}.
 * - Índice por {@code difficulty_id}.
 *
 * Conversores:
 * - El campo {@link #pcp} utiliza {@link PCPConverter} para persistir un arreglo de {@code double}.
 */
@Entity(
        tableName = "chords",
        foreignKeys = {
                @ForeignKey(entity = ChordType.class, parentColumns = "id", childColumns = "type_id", onDelete = CASCADE),
                @ForeignKey(entity = Difficulty.class, parentColumns = "id", childColumns = "difficulty_id", onDelete = CASCADE)
        },
        indices = {@Index("type_id"), @Index("difficulty_id")}
)
public class Chord {

    /** Clave primaria autogenerada del acorde. */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Nombre del acorde (no nulo), por ejemplo “Cmaj7”, “Am”, etc. */
    @NonNull
    public String name;

    /** Texto de ayuda o descripción breve del acorde. */
    public String hint;

    /** Sugerencia de digitación para el acorde. */
    @ColumnInfo(name = "finger_hint")
    public String fingerHint;

    /** Id del tipo de acorde, FK hacia {@link ChordType#id}. */
    @ColumnInfo(name = "type_id")
    public int typeId;

    /** Id del nivel de dificultad, FK hacia {@link Difficulty#id}. */
    @ColumnInfo(name = "difficulty_id")
    public int difficultyId;

    /**
     * Vector PCP (Pitch Class Profile) asociado al acorde.
     * Persistido mediante {@link PCPConverter}.
     */
    @TypeConverters(PCPConverter.class)
    public double[] pcp;

    /**
     * Constructor principal de la entidad.
     *
     * @param name        nombre del acorde (no nulo).
     * @param pcp         vector PCP asociado.
     * @param hint        ayuda o descripción del acorde.
     * @param fingerHint  sugerencia de digitación.
     * @param typeId      id del tipo de acorde (FK).
     * @param difficultyId id del nivel de dificultad (FK).
     */
    public Chord(@NonNull String name, double[] pcp, String hint, String fingerHint, int typeId, int difficultyId) {
        this.name = name;
        this.pcp = pcp;
        this.hint = hint;
        this.fingerHint = fingerHint;
        this.typeId = typeId;
        this.difficultyId = difficultyId;
    }

    /** @return nombre del acorde. */
    @NonNull
    public String getName() {
        return name;
    }

    /** Establece el nombre del acorde. */
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /** @return texto de ayuda. */
    public String getHint() {
        return hint;
    }

    /** Establece el texto de ayuda. */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /** @return identificador primario del acorde. */
    public int getId() {
        return id;
    }

    /** @return identificador del tipo de acorde (FK). */
    public int getChordTypeId() {
        return typeId;
    }

    /** Establece el id primario del acorde. */
    public void setId(int id) {
        this.id = id;
    }

    /** Establece el id del tipo de acorde (alias de {@link #setTypeId(int)}). */
    public void setChordTypeId(int chordTypeId) {
        this.typeId = chordTypeId;
    }

    /** @return sugerencia de digitación. */
    public String getFingerHint() {
        return fingerHint;
    }

    /** Establece la sugerencia de digitación. */
    public void setFingerHint(String fingerHint) {
        this.fingerHint = fingerHint;
    }

    /** @return id del tipo de acorde (FK). */
    public int getTypeId() {
        return typeId;
    }

    /** Establece el id del tipo de acorde (FK). */
    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    /** @return id de la dificultad (FK). */
    public int getDifficultyId() {
        return difficultyId;
    }

    /** Establece el id de la dificultad (FK). */
    public void setDifficultyId(int difficultyId) {
        this.difficultyId = difficultyId;
    }

    /** @return vector PCP asociado al acorde. */
    public double[] getPcp() {
        return pcp;
    }

    /** Establece el vector PCP. */
    public void setPcp(double[] pcp) {
        this.pcp = pcp;
    }
}
