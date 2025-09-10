package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Contrato del repositorio de logros.
 *
 * Define operaciones de lectura reactiva, evaluación de reglas de logro
 * y actualización del progreso tanto a nivel individual como en lote.
 * La implementación concreta debe coordinar las fuentes de datos (Room, memoria,
 * servicios) y garantizar la coherencia entre definiciones y estados.
 */
public interface AchievementRepository {

        /**
         * Observa todas las familias/niveles de logros disponibles en el dominio.
         *
         * @return un {@link LiveData} que emite listas de {@link Achievement} cuando cambia el origen de datos.
         */
        LiveData<List<Achievement>> observeAll();

        /**
         * Evalúa todas las reglas de logro y actualiza estados/progresos cuando proceda.
         * La implementación típica recalcula estados a partir de métrica(s) actuales
         * y aplica los cambios persistiendo el resultado.
         */
        void evaluateAll();

        /**
         * Actualiza en bloque una colección de logros, sustituyendo su estado/progreso
         * por los valores suministrados.
         *
         * @param achievements lista de logros a actualizar; no debe ser nula.
         */
        void updateAchievements(@NonNull List<Achievement> achievements);

        /**
         * Actualiza un logro concreto, sustituyendo su estado/progreso por el valor suministrado.
         *
         * @param achievement logro a actualizar; no debe ser nulo.
         */
        void updateAchievement(@NonNull Achievement achievement);

        /**
         * Obtiene el progreso actual para una familia y nivel determinados.
         *
         * @param familyTitle título de la familia de logros.
         * @param level       nivel del logro (BRONZE, SILVER, GOLD).
         * @return el valor de progreso actual (entero no negativo).
         */
        int getProgress(@NonNull String familyTitle, @NonNull Achievement.Level level);

        /**
         * Incrementa el progreso asociado a un código/identificador de logro de dominio.
         * La implementación debe manejar la saturación en el umbral correspondiente.
         *
         * @param code  código o identificador del logro a incrementar.
         * @param delta incremento a aplicar; se espera no negativo.
         */
        void incrementProgress(@NonNull String code, int delta);
}
