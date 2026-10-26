package com.example.huellitasya.core.data.di

import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.data.download.DownloadManagerRecordDownloader
import com.example.huellitasya.core.data.network.ConnectivityNetworkMonitor
import com.example.huellitasya.core.data.repository.FirebaseAuthRepository
import com.example.huellitasya.core.data.repository.FunctionsAccountRepository
import com.example.huellitasya.core.data.repository.FunctionsEmailChangeRepository
import com.example.huellitasya.core.data.repository.FirebaseStoragePetPhotoRepository
import com.example.huellitasya.core.data.repository.FirebaseStorageProfilePhotoRepository
import com.example.huellitasya.core.data.repository.FirestorePetRepository
import com.example.huellitasya.core.data.repository.FirestoreRecordRepository
import com.example.huellitasya.core.data.repository.FirebaseStorageRecordFileRepository
import com.example.huellitasya.core.data.repository.FirestoreUserRepository
import com.example.huellitasya.core.data.repository.FunctionsPasswordResetRepository
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.repository.PasswordResetRepository
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.domain.repository.RecordDownloader
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.domain.repository.RecordRepository
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.example.huellitasya.core.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        // Correos de Auth (recuperar contraseña, etc.) en español, sin depender de la plantilla de la consola.
        setLanguageCode("es")
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Misma región que las Cloud Functions de `functions/` y que Firestore. */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance(FUNCTIONS_REGION)

    /** Bucket por defecto del proyecto (fotos de perfil y de mascotas, specs 004 y 005). */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindPasswordResetRepository(impl: FunctionsPasswordResetRepository): PasswordResetRepository

    @Binds
    @Singleton
    abstract fun bindProfilePhotoRepository(impl: FirebaseStorageProfilePhotoRepository): ProfilePhotoRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: FunctionsAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindEmailChangeRepository(impl: FunctionsEmailChangeRepository): EmailChangeRepository

    @Binds
    @Singleton
    abstract fun bindPetRepository(impl: FirestorePetRepository): PetRepository

    @Binds
    @Singleton
    abstract fun bindPetPhotoRepository(impl: FirebaseStoragePetPhotoRepository): PetPhotoRepository

    @Binds
    @Singleton
    abstract fun bindRecordRepository(impl: FirestoreRecordRepository): RecordRepository

    @Binds
    @Singleton
    abstract fun bindRecordFileRepository(impl: FirebaseStorageRecordFileRepository): RecordFileRepository

    @Binds
    @Singleton
    abstract fun bindRecordDownloader(impl: DownloadManagerRecordDownloader): RecordDownloader
}

private const val FUNCTIONS_REGION = "southamerica-east1"
