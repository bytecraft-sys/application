package com.yourapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserProfile>>
}
