package com.magicvector.domain.convertor.base


/**
 * 通用转换器抽象类
 * @param TModel 业务模型
 * @param TEntity 数据库实体
 * @param TDto 网络数据传输对象
 */
abstract class BaseConvertor<TModel, TEntity, TDto> {

    // Model <-> Entity
    abstract fun model2Entity(model: TModel, id: Long? = null): TEntity
    abstract fun entity2Model(entity: TEntity): TModel

    // Model <-> Dto
    abstract fun model2Dto(model: TModel): TDto
    abstract fun dto2Model(dto: TDto): TModel

    // 批量转换
    fun dtos2Models(dtos: List<TDto>): List<TModel> = dtos.map { dto2Model(it) }
    fun models2Dtos(models: List<TModel>): List<TDto> = models.map { model2Dto(it) }
    fun entities2Models(entities: List<TEntity>): List<TModel> = entities.map { entity2Model(it) }
    fun models2Entities(models: List<TModel>): List<TEntity> = models.map { model2Entity(it) }
}