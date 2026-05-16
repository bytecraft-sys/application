package com.yourapp.migration

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.yourapp.data.local.AppDatabase
import com.yourapp.data.local.LocalStorageNames.ROOM_DATABASE_NAME
import com.yourapp.data.local.LocalStorageNames.USER_PROFILE_DATASTORE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class MigrationBundleManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val crypto: MigrationCrypto,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun prepareEncryptedBundle(
        uid: String,
        nowUnixEpoch: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
    ): PreparedMigrationBundle {
        val bundleUuid = UUID.randomUUID().toString()
        val ttlUnixEpoch = nowUnixEpoch + TTL_SECONDS
        val workDir = File(context.cacheDir, "migration/$bundleUuid").apply { mkdirs() }
        val manifest = MigrationManifest(
            schemaVersion = AppDatabase.VERSION,
            appVersion = appVersionName(),
            bundleUuid = bundleUuid,
            ttlUnixEpoch = ttlUnixEpoch,
        )
        val zipFile = File(workDir, "migration.zip")
        val encryptedFile = File(workDir, "$bundleUuid.enc")

        checkpointAndCloseDatabase()
        writeZipBundle(
            zipFile = zipFile,
            manifest = manifest,
        )
        crypto.encryptZip(
            uid = uid,
            bundleUuid = bundleUuid,
            zipFile = zipFile,
            encryptedFile = encryptedFile,
        )

        return PreparedMigrationBundle(
            bundleUuid = bundleUuid,
            ttlUnixEpoch = ttlUnixEpoch,
            encryptedFile = encryptedFile,
            storagePath = firebaseStoragePath(uid, bundleUuid),
        )
    }

    fun restoreEncryptedBundle(
        uid: String,
        encryptedFile: File,
        expectedBundleUuid: String,
        nowUnixEpoch: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
    ): MigrationRestoreResult {
        if (wasRestored(expectedBundleUuid)) {
            return MigrationRestoreResult.SkippedAlreadyRestored(expectedBundleUuid)
        }

        val workDir = File(context.cacheDir, "migration/restore/$expectedBundleUuid").apply { mkdirs() }
        val decryptedZip = File(workDir, "migration.zip")
        val (bundleUuid, zipFile) = crypto.decryptZip(
            uid = uid,
            encryptedFile = encryptedFile,
            destinationZipFile = decryptedZip,
        )

        if (bundleUuid != expectedBundleUuid) {
            throw MigrationRejectedException("Migration bundle id does not match the selected remote object.")
        }

        val extractedDir = File(workDir, "extracted").apply { mkdirs() }
        val manifest = unzipBundle(zipFile, extractedDir)

        if (manifest.ttlUnixEpoch <= nowUnixEpoch) {
            return MigrationRestoreResult.Expired
        }

        replaceLocalFiles(extractedDir)
        validateRoomDatabase()
        markRestored(bundleUuid)

        return MigrationRestoreResult.Restored(bundleUuid)
    }

    private fun checkpointAndCloseDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            while (cursor.moveToNext()) {
                // Exhaust the cursor so SQLite completes the checkpoint before close.
            }
        }
        database.close()
    }

    private fun writeZipBundle(
        zipFile: File,
        manifest: MigrationManifest,
    ) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry(ROOM_ENTRY))
            FileInputStream(context.getDatabasePath(ROOM_DATABASE_NAME)).use { input ->
                input.copyTo(zip)
            }
            zip.closeEntry()

            val dataStoreFile = context.dataStoreFile(USER_PROFILE_DATASTORE_NAME)
            if (dataStoreFile.exists()) {
                zip.putNextEntry(ZipEntry(DATASTORE_ENTRY))
                FileInputStream(dataStoreFile).use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }

            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(json.encodeToString(MigrationManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
        }
    }

    private fun unzipBundle(
        zipFile: File,
        destinationDir: File,
    ): MigrationManifest {
        var manifest: MigrationManifest? = null
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outputFile = when (entry.name) {
                    ROOM_ENTRY -> File(destinationDir, ROOM_ENTRY)
                    DATASTORE_ENTRY -> File(destinationDir, DATASTORE_ENTRY)
                    MANIFEST_ENTRY -> File(destinationDir, MANIFEST_ENTRY)
                    else -> throw MigrationRejectedException("Unexpected migration entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    throw MigrationRejectedException("Migration bundle cannot contain directories.")
                }

                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile).use { output ->
                    zip.copyTo(output)
                }

                if (entry.name == MANIFEST_ENTRY) {
                    manifest = json.decodeFromString(
                        MigrationManifest.serializer(),
                        outputFile.readText(),
                    )
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return manifest ?: throw MigrationRejectedException("Migration manifest is missing.")
    }

    private fun replaceLocalFiles(extractedDir: File) {
        database.close()
        val restoredDb = File(extractedDir, ROOM_ENTRY)
        if (!restoredDb.exists()) {
            throw MigrationRejectedException("Migration database file is missing.")
        }

        val targetDb = context.getDatabasePath(ROOM_DATABASE_NAME)
        targetDb.parentFile?.mkdirs()
        deleteDatabaseFiles(targetDb)
        restoredDb.copyTo(targetDb, overwrite = true)

        val restoredDataStore = File(extractedDir, DATASTORE_ENTRY)
        if (restoredDataStore.exists()) {
            val targetDataStore = context.dataStoreFile(USER_PROFILE_DATASTORE_NAME)
            targetDataStore.parentFile?.mkdirs()
            restoredDataStore.copyTo(targetDataStore, overwrite = true)
        }
    }

    private fun validateRoomDatabase() {
        try {
            val restoredDatabase = Room.databaseBuilder(
                context,
            AppDatabase::class.java,
            ROOM_DATABASE_NAME,
        )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            restoredDatabase.openHelper.writableDatabase
            restoredDatabase.close()
        } catch (exception: RuntimeException) {
            deleteDatabaseFiles(context.getDatabasePath(ROOM_DATABASE_NAME))
            throw exception
        }
    }

    private fun deleteDatabaseFiles(databaseFile: File) {
        listOf(
            databaseFile,
            File("${databaseFile.path}-wal"),
            File("${databaseFile.path}-shm"),
        ).forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun wasRestored(bundleUuid: String): Boolean {
        return restorePrefs().getBoolean(bundleUuid, false)
    }

    private fun markRestored(bundleUuid: String) {
        restorePrefs().edit {
            putBoolean(bundleUuid, true)
        }
    }

    private fun restorePrefs() = context.getSharedPreferences(
        RESTORE_PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    @Suppress("DEPRECATION")
    private fun appVersionName(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "unknown"
    }

    companion object {
        const val TTL_SECONDS = 24 * 60 * 60L

        fun firebaseStoragePath(uid: String, bundleUuid: String): String {
            return "migrations/$uid/$bundleUuid.enc"
        }

        private const val MILLIS_PER_SECOND = 1_000L
        private const val RESTORE_PREFS_NAME = "migration_restore"
        private const val ROOM_ENTRY = "room.db"
        private const val DATASTORE_ENTRY = "datastore.pb"
        private const val MANIFEST_ENTRY = "manifest.json"
    }
}
