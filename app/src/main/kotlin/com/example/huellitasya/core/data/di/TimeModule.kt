package com.example.huellitasya.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Reloj del sistema. Los ViewModels que dependen de "hoy" lo reciben inyectado para poder fijarlo en los tests. */
@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
