package com.todoacorde.todoacorde;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO de acceso a usuarios.
 *
 * Incluye operaciones básicas de inserción, consulta por id, listado
 * y borrado de entidades {@link User}.
 */
@Dao
public interface UserDao {

    /**
     * Inserta o reemplaza un usuario.
     *
     * @param user entidad {@link User} a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    /**
     * Obtiene un usuario por su identificador.
     *
     * @param id id del usuario.
     * @return usuario encontrado o {@code null} si no existe.
     */
    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    User getUserById(int id);

    /**
     * Devuelve todos los usuarios existentes.
     *
     * @return lista de usuarios.
     */
    @Query("SELECT * FROM User")
    List<User> getAllUsers();

    /**
     * Elimina el usuario indicado.
     *
     * @param user entidad a borrar.
     */
    @Delete
    void deleteUser(User user);
}
