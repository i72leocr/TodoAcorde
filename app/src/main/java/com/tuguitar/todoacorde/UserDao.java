package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM User")
    List<User> getAllUsers();

    @Delete
    void deleteUser(User user);


}
