package com.yourapp.ui.history

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        isYesterday() -> "Yesterday"
        diff < 7 * 86400_000 -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(this))
        }
        else -> {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(this))
        }
    }
}

private fun Long.isYesterday(): Boolean {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = this@isYesterday }
    now.add(Calendar.DAY_OF_YEAR, -1)
    return now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
}
