package com.vectordemo.dataSource.local.db

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vectordemo.database.VectorDatabase
import com.vectordemo.domain.config.requireApplicationContext

actual fun createVectorDatabase(): VectorDatabase {
    val context = requireApplicationContext()
    val driver = AndroidSqliteDriver(
        schema = VectorDatabase.Schema,
        context = context,
        name = "vector_demo.db",
    )
    return VectorDatabase(driver)
}
