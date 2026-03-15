package com.magicvector.domain.convertor

import com.magicvector.domain.entity.UserEntity
import com.magicvector.domain.model.UserSessionModel

object UserConvertor {
    fun model2Entity(session: UserSessionModel): UserEntity {
        return UserEntity(
            userId = session.userId,
            account = session.account,
            name = session.name,
            avatarUrl = session.avatarUrl,
            accessToken = session.accessToken,
            password = session.password,
            isCurrent = session.isCurrent,
            lastLoginAt = session.lastLoginAt
        )
    }

    fun entity2Model(entity: UserEntity): UserSessionModel {
        return UserSessionModel(
            userId = entity.userId,
            account = entity.account,
            name = entity.name,
            avatarUrl = entity.avatarUrl,
            accessToken = entity.accessToken,
            password = entity.password,
            isCurrent = entity.isCurrent,
            lastLoginAt = entity.lastLoginAt
        )
    }
}
