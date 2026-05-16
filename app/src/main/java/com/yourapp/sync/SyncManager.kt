package com.yourapp.sync

import android.content.Context
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun enqueueChatSync(threshold: Long = System.currentTimeMillis()) {
        WorkManager.getInstance(context).enqueue(SyncWorker.buildRequest(threshold))
    }
}
