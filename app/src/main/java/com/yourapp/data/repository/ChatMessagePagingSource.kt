package com.yourapp.data.repository

import androidx.paging.PagingSource
import com.yourapp.data.local.ChatMessage
import com.yourapp.data.local.ChatMessageDao
import javax.inject.Inject

class ChatMessagePagingSource @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
) {
    fun pagingSource(): PagingSource<Int, ChatMessage> = chatMessageDao.pagingSource()
}
