package com.vectordemo.dataSource.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vectordemo.domain.entity.OssUserBucketFileEntity
import com.vectordemo.domain.entity.UserEntity
import com.vectordemo.repository.dao.OssUserBucketFileDao
import com.vectordemo.repository.dao.UserDao

@Database(
    entities = [UserEntity::class, OssUserBucketFileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VectorDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun ossUserBucketFileDao(): OssUserBucketFileDao

    companion object {
        private const val DB_NAME = "vector_demo.db"
        @Volatile
        private var INSTANCE: VectorDatabase? = null

        fun getInstance(context: Context): VectorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VectorDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
