package com.todoacorde.todoacorde;

import com.todoacorde.todoacorde.achievements.data.AchievementRepository;
import com.todoacorde.todoacorde.achievements.data.AchievementRepositoryImpl;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Módulo de Dagger/Hilt que declara los mapeos de interfaces a sus implementaciones
 * mediante @Binds y la provisión de utilidades compartidas mediante @Provides.
 *
 * <p>Se instala en {@link SingletonComponent}, por lo que los objetos expuestos
 * tendrán ciclo de vida de aplicación.</p>
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class DataBindingsModule {

    /**
     * Vincula la interfaz {@link IChordDetector} con su implementación {@link ChordDetectorImpl}.
     *
     * @param impl implementación concreta a inyectar cuando se requiera {@link IChordDetector}.
     * @return instancia enlazada como {@link IChordDetector}.
     */
    @Binds
    @Singleton
    public abstract IChordDetector bindChordDetector(ChordDetectorImpl impl);

    /**
     * Vincula la interfaz {@link AchievementRepository} con su implementación
     * {@link AchievementRepositoryImpl}.
     *
     * @param impl implementación concreta a inyectar cuando se requiera {@link AchievementRepository}.
     * @return instancia enlazada como {@link AchievementRepository}.
     */
    @Binds
    @Singleton
    public abstract AchievementRepository bindAchievementRepository(
            AchievementRepositoryImpl impl
    );

    /**
     * Proporciona un singleton de {@link AppExecutors} reutilizable en toda la app.
     *
     * <p>Se usa @Provides porque la instancia proviene de un método de factoría estático
     * en lugar de un constructor inyectable.</p>
     *
     * @return instancia global de {@link AppExecutors}.
     */
    @Provides
    @Singleton
    public static AppExecutors provideAppExecutors() {
        return AppExecutors.getInstance();
    }
}
