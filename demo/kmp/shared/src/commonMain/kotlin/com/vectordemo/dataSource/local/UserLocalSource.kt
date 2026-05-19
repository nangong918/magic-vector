package com.vectordemo.dataSource.local

import com.vectordemo.database.User_session
import com.vectordemo.database.VectorDatabase
import com.vectordemo.domain.convertor.UserConvertor
import com.vectordemo.domain.entity.UserEntity
import com.vectordemo.domain.model.user.UserSessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 用户本地数据源：对外仅 [UserSessionModel]，内部用 Entity + [UserConvertor]。
 */
class UserLocalSource(
    private val database: VectorDatabase,
) {
    private val queries get() = database.userSessionQueries

    suspend fun saveCurrentUser(session: UserSessionModel) = withContext(Dispatchers.IO) {
        queries.clearCurrentFlag()
        queries.upsertUserSession(
            user_id = session.userId,
            account = session.account,
            name = session.name,
            avatar_url = session.avatarUrl,
            access_token = session.accessToken,
            password = session.password,
            is_current = if (session.isCurrent) 1L else 0L,
            last_login_at = session.lastLoginAt,
        )
    }

    suspend fun getCurrentUser(): UserSessionModel? = withContext(Dispatchers.IO) {
        queries.getCurrentUser().executeAsOneOrNull()?.toEntity()?.let(UserConvertor::entity2Model)
    }

    suspend fun getAllUsers(): List<UserSessionModel> = withContext(Dispatchers.IO) {
        queries.getAllUsers().executeAsList().map { UserConvertor.entity2Model(it.toEntity()) }
    }

    suspend fun clearCurrentUser() = withContext(Dispatchers.IO) {
        queries.getCurrentUser().executeAsOneOrNull()?.toEntity()?.let { entity ->
            queries.upsertUserSession(
                user_id = entity.userId,
                account = entity.account,
                name = entity.name,
                avatar_url = entity.avatarUrl,
                access_token = "",
                password = entity.password,
                is_current = 0L,
                last_login_at = entity.lastLoginAt,
            )
        }
    }

    private fun User_session.toEntity(): UserEntity = UserEntity(
        id = id,
        userId = user_id,
        account = account,
        name = name,
        avatarUrl = avatar_url,
        accessToken = access_token,
        password = password,
        isCurrent = is_current != 0L,
        lastLoginAt = last_login_at,
    )
}
