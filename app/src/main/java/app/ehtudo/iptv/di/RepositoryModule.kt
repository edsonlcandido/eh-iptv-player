package app.ehtudo.iptv.di

import app.ehtudo.data.local.DatabaseTransactionRunner
import app.ehtudo.data.local.RoomDatabaseTransactionRunner
import app.ehtudo.data.manager.DownloadManagerImpl
import app.ehtudo.data.preferences.PreferencesRepository
import app.ehtudo.data.security.AndroidKeystoreCredentialCrypto
import app.ehtudo.data.security.CredentialCrypto
import app.ehtudo.data.sync.ProviderSyncStateReaderImpl
import app.ehtudo.data.validation.ProviderSetupInputValidatorImpl
import app.ehtudo.domain.manager.ParentalPinVerifier
import app.ehtudo.domain.manager.ProviderSetupInputValidator
import app.ehtudo.domain.manager.ProviderSyncStateReader
import app.ehtudo.data.repository.*
import app.ehtudo.domain.manager.ParentalControlSessionStore
import app.ehtudo.domain.repository.*
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
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProviderRepository(impl: ProviderRepositoryImpl): ProviderRepository

    @Binds @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds @Singleton
    abstract fun bindCombinedM3uRepository(impl: CombinedM3uRepositoryImpl): CombinedM3uRepository

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds @Singleton
    abstract fun bindEpgSourceRepository(impl: EpgSourceRepositoryImpl): EpgSourceRepository

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindPlaybackHistoryRepository(impl: PlaybackHistoryRepositoryImpl): PlaybackHistoryRepository

    @Binds @Singleton
    abstract fun bindExternalRatingsRepository(impl: ExternalRatingsRepositoryImpl): ExternalRatingsRepository

    @Binds @Singleton
    abstract fun bindSyncMetadataRepository(impl: SyncMetadataRepositoryImpl): SyncMetadataRepository

    @Binds @Singleton
    abstract fun bindPlaybackCompatibilityRepository(impl: PlaybackCompatibilityRepositoryImpl): PlaybackCompatibilityRepository

    @Binds @Singleton
    abstract fun bindDatabaseTransactionRunner(impl: RoomDatabaseTransactionRunner): DatabaseTransactionRunner

    @Binds @Singleton
    abstract fun bindBackupManager(impl: app.ehtudo.data.manager.BackupManagerImpl): app.ehtudo.domain.manager.BackupManager

    @Binds @Singleton
    abstract fun bindDriveBackupSyncManager(impl: app.ehtudo.data.manager.GoogleDriveBackupSyncManager): app.ehtudo.domain.manager.DriveBackupSyncManager

    @Binds @Singleton
    abstract fun bindRecordingManager(impl: app.ehtudo.data.manager.RecordingManagerImpl): app.ehtudo.domain.manager.RecordingManager

    @Binds @Singleton
    abstract fun bindDownloadManager(impl: DownloadManagerImpl): DownloadManager

    @Binds @Singleton
    abstract fun bindProgramReminderManager(impl: app.ehtudo.data.manager.ProgramReminderManagerImpl): app.ehtudo.domain.manager.ProgramReminderManager

    @Binds @Singleton
    abstract fun bindParentalControlSessionStore(impl: PreferencesRepository): ParentalControlSessionStore

    @Binds @Singleton
    abstract fun bindParentalPinVerifier(impl: PreferencesRepository): ParentalPinVerifier

    @Binds @Singleton
    abstract fun bindProviderSetupInputValidator(impl: ProviderSetupInputValidatorImpl): ProviderSetupInputValidator

    @Binds @Singleton
    abstract fun bindProviderSyncStateReader(impl: ProviderSyncStateReaderImpl): ProviderSyncStateReader

    @Binds @Singleton
    abstract fun bindCredentialCrypto(impl: AndroidKeystoreCredentialCrypto): CredentialCrypto

    companion object {
        @Provides
        @Singleton
        fun provideRepositoryCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        @Provides
        @Singleton
        fun provideM3uParser(): app.ehtudo.data.parser.M3uParser {
            return app.ehtudo.data.parser.M3uParser()
        }
    }
}
