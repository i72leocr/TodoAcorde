package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "chords",
        foreignKeys = {
                @ForeignKey(entity = ChordType.class, parentColumns = "id", childColumns = "type_id", onDelete = CASCADE),
                @ForeignKey(entity = Difficulty.class, parentColumns = "id", childColumns = "difficulty_id", onDelete = CASCADE)
        },
        indices = {@Index("type_id"), @Index("difficulty_id")}
)
public class Chord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    public String hint;

    @ColumnInfo(name = "finger_hint")
    public String fingerHint; // Nueva pista indicando los números de los dedos

    @ColumnInfo(name = "type_id")
    public int typeId; // Clave foránea hacia ChordType

    @ColumnInfo(name = "difficulty_id")
    public int difficultyId; // Clave foránea hacia Difficulty

    @TypeConverters(PCPConverter.class)
    public double[] pcp;

    // Constructor
    public Chord(@NonNull String name, double[] pcp, String hint, String fingerHint, int typeId, int difficultyId) {
        this.name = name;
        this.pcp = pcp;
        this.hint = hint;
        this.fingerHint = fingerHint;
        this.typeId = typeId;
        this.difficultyId = difficultyId;
    }

    // Métodos getter y setter
    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
    // Getters
    public int getId() {
        return id;
    }



    public int getChordTypeId() {
        return typeId;
    }



    // Setters
    public void setId(int id) {
        this.id = id;
    }



    public void setChordTypeId(int chordTypeId) {
        this.typeId = chordTypeId;
    }


    public String getFingerHint() {
        return fingerHint;
    }

    public void setFingerHint(String fingerHint) {
        this.fingerHint = fingerHint;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getDifficultyId() {
        return difficultyId;
    }

    public void setDifficultyId(int difficultyId) {
        this.difficultyId = difficultyId;
    }

    public double[] getPcp() {
        return pcp;
    }

    public void setPcp(double[] pcp) {
        this.pcp = pcp;
    }

}
