package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "chords")
public class Chord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String hint;

    @TypeConverters(PCPConverter.class)
    public double[] pcp;

    public Chord(String name, double[] pcp, String hint) {
        this.name = name;
        this.pcp = pcp;
        this.hint = hint;
    }

    // Métodos getter para name y hint
    public String getName() {
        return name;
    }

    public String getHint() {
        return hint;
    }

    public double[] getPcp() {
        return pcp;
    }

}
