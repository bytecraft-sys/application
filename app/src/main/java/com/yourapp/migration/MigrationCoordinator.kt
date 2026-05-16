package com.yourapp.migration

import com.yourapp.sync.SyncManager
import java.io.File
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationCoordinator @Inject constructor(
    private val bundleManager: MigrationBundleManager,
    private val storageClient: MigrationStorageClient,
    private val syncManager: SyncManager,
) {
    suspend fun exportForSignedInUser(uid: String): MigrationExportResult {
        val bundle = bundleManager.prepareEncryptedBundle(uid = uid)
        storageClient.upload(uid = uid, bundle = bundle)
        return MigrationExportResult.Uploaded(bundle)
    }

    suspend fun restoreLatestForSignedInUser(uid: String): MigrationRestoreResult {
        val now = System.currentTimeMillis() / MILLIS_PER_SECOND
        val remoteBundle = storageClient.findLatestValidBundle(
            uid = uid,
            nowUnixEpoch = now,
        ) ?: return MigrationRestoreResult.NoBundleFound

        if (remoteBundle.ttlUnixEpoch <= now) {
            return MigrationRestoreResult.Expired
        }

        val encryptedDownload = File.createTempFile("migration-${remoteBundle.bundleUuid}", ".enc")
        return try {
            storageClient.download(
                remoteBundle = remoteBundle,
                destination = encryptedDownload,
            )
            val result = bundleManager.restoreEncryptedBundle(
                uid = uid,
                encryptedFile = encryptedDownload,
                expectedBundleUuid = remoteBundle.bundleUuid,
                nowUnixEpoch = now,
            )
            if (result is MigrationRestoreResult.Restored) {
                syncManager.enqueueChatSync()
            }
            result
        } catch (exception: AEADBadTagException) {
            throw MigrationRejectedException("Migration bundle could not be authenticated.", exception)
        } finally {
            encryptedDownload.delete()
        }
    }

    private companion object {
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
