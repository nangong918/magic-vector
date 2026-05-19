package com.vectordemo.domain.convertor

import com.vectordemo.domain.entity.UserEntity
import com.vectordemo.domain.model.user.UserSessionModel

object UserConvertor {
    fun model2Entity(session: UserSessionModel): UserEntity = UserEntity(
        userId = session.userId,
        account = session.account,
        name = session.name,
        avatarUrl = session.avatarUrl,
        accessToken = session.accessToken,
        password = session.password,
        isCurrent = session.isCurrent,
        lastLoginAt = session.lastLoginAt,
    )

    fun entity2Model(entity: UserEntity): UserSessionModel = UserSessionModel(
        userId = entity.userId,
        account = entity.account,
        name = entity.name,
        avatarUrl = entity.avatarUrl,
        accessToken = entity.accessToken,
        password = entity.password,
        isCurrent = entity.isCurrent,
        lastLoginAt = entity.lastLoginAt,
    )
}
