package com.yourapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatMessage::class,
        ChatSession::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(
    MessageMetaConverter::class,
    StringListConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        const val VERSION = 2

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create chat_sessions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `updatedAt` INTEGER NOT NULL, 
                        `messageCount` INTEGER NOT NULL, 
                        `lastMessage` TEXT NOT NULL, 
                        `isPinned` INTEGER NOT NULL, 
                        `isDeleted` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                
                // 2. Create new chat_messages table with the correct schema including Foreign Key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages_new` (
                        `id` TEXT NOT NULL, 
                        `sessionId` TEXT NOT NULL, 
                        `content` TEXT NOT NULL, 
                        `role` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `isSynced` INTEGER NOT NULL, 
                        `meta` TEXT NOT NULL, 
                        `lastSyncedAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                
                // 3. Copy data from old table to new table. 
                // Since sessionId is NOT NULL, we'll assign a dummy empty string for now.
                // Note: Foreign key constraints are usually disabled during migration.
                db.execSQL("""
                    INSERT INTO `chat_messages_new` (id, sessionId, content, role, timestamp, isSynced, meta, lastSyncedAt)
                    SELECT id, '', content, role, timestamp, isSynced, meta, lastSyncedAt FROM `chat_messages`
                """.trimIndent())
                
                // 4. Remove old table
                db.execSQL("DROP TABLE `chat_messages`")
                
                // 5. Rename new table to original name
                db.execSQL("ALTER TABLE `chat_messages_new` RENAME TO `chat_messages`")
                
                // 6. Re-create indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_lastSyncedAt` ON `chat_messages` (`lastSyncedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)")
            }
        }
    }
}
