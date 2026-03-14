package com.magicvector.manager.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEntity)

    @Query("SELECT * FROM user_session WHERE is_current = 1 ORDER BY last_login_at DESC LIMIT 1")
    suspend fun getCurrent(): UserEntity?

    @Query("SELECT * FROM user_session ORDER BY is_current DESC, last_login_at DESC, id DESC")
    suspend fun getAll(): List<UserEntity>

    @Query("UPDATE user_session SET is_current = 0")
    suspend fun clearCurrentFlag()

    @Query("DELETE FROM user_session")
    suspend fun deleteAll()
}
