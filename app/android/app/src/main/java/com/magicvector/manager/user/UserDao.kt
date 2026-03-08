package com.magicvector.manager.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEntity)

    @Query("SELECT * FROM user_session WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long = UserEntity.CURRENT_ROW_ID): UserEntity?

    @Query("DELETE FROM user_session WHERE id = :id")
    suspend fun deleteById(id: Long = UserEntity.CURRENT_ROW_ID)
}
