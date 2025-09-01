/*
 * FavoriteSong.java
 * Entity representing the join between User and Song for favorites.
 * - Composite primary key on (userId, songId).
 * - Foreign keys ensure referential integrity with CASCADE deletes.
 * - Consider adding @Index on songId if querying by song frequently.
 */
package com.tuguitar.todoacorde.songs.data;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.tuguitar.todoacorde.User;

@Entity(
        tableName = "favorite_songs",
        primaryKeys = {"userId", "songId"},
        indices = {@Index(value = "songId" )}, // improve lookup performance
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = CASCADE
                )
        }
)
public class FavoriteSong {
    public final int userId;
    public final int songId;

    /**
     * @param userId ID of the user marking a favorite.
     * @param songId ID of the song being favorited.
     */
    public FavoriteSong(int userId, int songId) {
        this.userId = userId;
        this.songId = songId;
    }
}

