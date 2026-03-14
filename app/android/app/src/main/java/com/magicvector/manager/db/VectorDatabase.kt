package com.magicvector.manager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.magicvector.manager.chat.AgentCacheDao
import com.magicvector.manager.chat.AgentCacheEntity
import com.magicvector.manager.chat.ChatMessageDao
import com.magicvector.manager.chat.ChatMessageEntity
import com.magicvector.manager.control.ControlAgentLogDao
import com.magicvector.manager.control.ControlAgentLogEntity
import com.magicvector.manager.user.UserDao
import com.magicvector.manager.user.UserEntity

/**
 * 全局唯一 Room 数据库（Vector）。
 * 业务表（如 User）作为该数据库的实体之一，不单独拆分数据库实例。
 */
@Database(
    entities = [
        UserEntity::class,
        AgentCacheEntity::class,
        ChatMessageEntity::class,
        ControlAgentLogEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class VectorDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun agentCacheDao(): AgentCacheDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun controlAgentLogDao(): ControlAgentLogDao

    companion object {
        private const val DB_NAME = "magic_vector.db"

        @Volatile
        private var INSTANCE: VectorDatabase? = null

        fun getInstance(context: Context): VectorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // 必须用applicationContext避免内存泄漏
                    VectorDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
