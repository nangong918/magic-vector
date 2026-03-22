package com.magicvector.dataSource.local.base

import com.magicvector.domain.convertor.base.BaseConvertor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通用本地数据源抽象类
 * @param TModel 业务模型类型
 * @param TEntity 数据库实体类型
 * @param TConvertor 转换器类型（继承BaseConvertor）
 */
abstract class BaseLocalSource<TModel, TEntity, TConvertor : BaseConvertor<TModel, TEntity, *>> {

    protected abstract val convertor: TConvertor

    // ========== 通用转换方法（直接使用convertor） ==========

    protected fun modelToEntity(model: TModel): TEntity = convertor.model2Entity(model)
    protected fun entityToModel(entity: TEntity): TModel = convertor.entity2Model(entity)
    protected fun entitiesToModels(entities: List<TEntity>): List<TModel> = convertor.entities2Models(entities)
    protected fun modelsToEntities(models: List<TModel>): List<TEntity> = convertor.models2Entities(models)

    // ========== 通用写入操作 ==========

    protected suspend fun upsert(
        model: TModel,
        upsertEntity: suspend (TEntity) -> Long
    ) = withContext(Dispatchers.IO) {
        upsertEntity(modelToEntity(model))
    }

    protected suspend fun upsertBatch(
        models: List<TModel>,
        upsertBatchEntity: suspend (List<TEntity>) -> List<Long>
    ) = withContext(Dispatchers.IO) {
        if (models.isNotEmpty()) {
            upsertBatchEntity(modelsToEntities(models))
        }
    }

    protected suspend fun delete(
        deleteAction: suspend () -> Unit
    ) = withContext(Dispatchers.IO) {
        deleteAction()
    }

    // ========== 通用查询操作 ==========

    protected suspend fun queryOne(
        query: suspend () -> TEntity?
    ): TModel? = withContext(Dispatchers.IO) {
        query()?.let { entityToModel(it) }
    }

    protected suspend fun queryList(
        query: suspend () -> List<TEntity>
    ): List<TModel> = withContext(Dispatchers.IO) {
        entitiesToModels(query())
    }
}