package com.yourapp.migration

sealed interface MigrationExportResult {
    data class Uploaded(val bundle: PreparedMigrationBundle) : MigrationExportResult
}

sealed interface MigrationRestoreResult {
    data object NoBundleFound : MigrationRestoreResult

    data class Restored(val bundleUuid: String) : MigrationRestoreResult

    data class SkippedAlreadyRestored(val bundleUuid: String) : MigrationRestoreResult

    data object Expired : MigrationRestoreResult
}

class MigrationRejectedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
