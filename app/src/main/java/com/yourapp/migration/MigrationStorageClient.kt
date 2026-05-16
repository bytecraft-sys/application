package com.yourapp.migration

import java.io.File

interface MigrationStorageClient {
    suspend fun upload(
        uid: String,
        bundle: PreparedMigrationBundle,
    )

    suspend fun findLatestValidBundle(
        uid: String,
        nowUnixEpoch: Long,
    ): MigrationRemoteBundle?

    suspend fun download(
        remoteBundle: MigrationRemoteBundle,
        destination: File,
    ): File
}
