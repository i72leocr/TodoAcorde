package com.tuguitar.todoacorde.scales.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity de nota en diapasón perteneciente a un patrón.
 * stringIndex: 0=6ª … 5=1ª ; degree: "R","b2","p4","p5", etc.
 */
@Entity(
        tableName = "scale_notes",
        foreignKeys = @ForeignKey(
                entity = ScalePatternEntity.class,
                parentColumns = "id",
                childColumns = "patternId",
                onDelete = CASCADE
        ),
        indices = {
                @Index("patternId"),
                @Index({"patternId","stringIndex"})
        }
)
public class ScaleNoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patternId;

    public int stringIndex;   // 0..5 (0=6ª)
    public int fret;          // traste
    public String degree;     // "R","b2","b3","p4","p5","b6","b7","3","7"...
    public boolean isRoot;    // true si es tónica
    public String noteName;   // "E","F","G#","Bb"...
    public String tag;        // opcional (null)

    public ScaleNoteEntity(long patternId, int stringIndex, int fret,
                           String degree, boolean isRoot, String tag, String noteName) {
        this.patternId = patternId;
        this.stringIndex = stringIndex;
        this.fret = fret;
        this.degree = degree;
        this.isRoot = isRoot;
        this.tag = tag;
        this.noteName = noteName;
    }
}
