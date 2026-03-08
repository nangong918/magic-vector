package com.magicvector.manager.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEntity)

    @Query("SELECT * FROM user_session WHERE session_key = :sessionKey LIMIT 1")
    suspend fun getBySessionKey(sessionKey: String = UserEntity.CURRENT_SESSION_KEY): UserEntity?

    @Query("DELETE FROM user_session WHERE session_key = :sessionKey")
    suspend fun deleteBySessionKey(sessionKey: String = UserEntity.CURRENT_SESSION_KEY)
}
