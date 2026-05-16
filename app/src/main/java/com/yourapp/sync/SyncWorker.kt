package com.yourapp.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yourapp.data.local.ChatMessageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chatMessageDao: ChatMessageDao,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val threshold = inputData.getLong(KEY_THRESHOLD_MILLIS, System.currentTimeMillis())
        val pendingMessages = chatMessageDao.observeUnsyncedBefore(threshold).first()

        if (pendingMessages.isEmpty()) {
            return Result.success()
        }

        val syncedAt = System.currentTimeMillis()
        val syncedMessages = pendingMessages.map { message ->
            message.copy(isSynced = true, lastSyncedAt = syncedAt)
        }

        chatMessageDao.updateAll(syncedMessages)
        return Result.success()
    }

    companion object {
        private const val KEY_THRESHOLD_MILLIS = "threshold_millis"

        fun buildRequest(threshold: Long = System.currentTimeMillis()): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(KEY_THRESHOLD_MILLIS to threshold))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }
    }
}
