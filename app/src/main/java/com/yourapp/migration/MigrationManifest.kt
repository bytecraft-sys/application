package com.yourapp.migration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MigrationManifest(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("app_version") val appVersion: String,
    @SerialName("bundle_uuid") val bundleUuid: String,
    @SerialName("ttl_unix_epoch") val ttlUnixEpoch: Long,
)

@Serializable
data class MigrationBundleHeader(
    @SerialName("bundle_uuid") val bundleUuid: String,
    @SerialName("iv_base64") val ivBase64: String,
)

data class MigrationRemoteBundle(
    val bundleUuid: String,
    val ttlUnixEpoch: Long,
    val storagePath: String,
)

data class PreparedMigrationBundle(
    val bundleUuid: String,
    val ttlUnixEpoch: Long,
    val encryptedFile: java.io.File,
    val storagePath: String,
)
