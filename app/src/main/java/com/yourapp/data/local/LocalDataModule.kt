package com.yourapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.yourapp.data.local.LocalStorageNames.ROOM_DATABASE_NAME
import com.yourapp.data.local.LocalStorageNames.USER_PROFILE_DATASTORE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        ROOM_DATABASE_NAME,
    )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideChatSessionDao(database: AppDatabase): ChatSessionDao = database.chatSessionDao()

    @Provides
    @Singleton
    fun provideUserProfileDataStore(
        @ApplicationContext context: Context,
    ): DataStore<UserProfile> = DataStoreFactory.create(
        serializer = UserProfileSerializer,
        produceFile = { context.dataStoreFile(USER_PROFILE_DATASTORE_NAME) },
    )
}
