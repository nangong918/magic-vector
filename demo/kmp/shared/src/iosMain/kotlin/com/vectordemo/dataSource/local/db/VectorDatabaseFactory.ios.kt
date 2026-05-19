package com.vectordemo.dataSource.local.db

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.vectordemo.database.VectorDatabase

actual fun createVectorDatabase(): VectorDatabase {
    val driver = NativeSqliteDriver(
        schema = VectorDatabase.Schema,
        name = "vector_demo.db",
    )
    return VectorDatabase(driver)
}
