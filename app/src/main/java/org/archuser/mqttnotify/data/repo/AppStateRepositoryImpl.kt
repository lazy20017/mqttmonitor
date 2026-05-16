package org.archuser.mqttnotify.data.repo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.data.local.AppStateDao
import org.archuser.mqttnotify.domain.model.AppState
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.repo.AppStateRepository

@Singleton
class AppStateRepositoryImpl @Inject constructor(
    private val appStateDao: AppStateDao,
    private val dispatchers: DispatchersProvider
) : AppStateRepository {

    override fun observeState(): Flow<AppState> = appStateDao.observeState().map { entity ->
        entity?.toModel() ?: defaultState
    }

    override suspend fun currentState(): AppState = withContext(dispatchers.io) {
        appStateDao.getState()?.toModel() ?: defaultState.also { appStateDao.upsert(it.toEntity()) }
    }

    override suspend fun setActiveBroker(id: Long?) = update { it.copy(activeBrokerId = id) }

    override suspend fun setConnectionMode(mode: ConnectionMode) = update { it.copy(connectionMode = mode) }

    override suspend fun setGlobalMuteUntil(until: Long?) = update { it.copy(globalMuteUntil = until) }

    override suspend fun setLastSessionStartedAt(time: Long?) = update { it.copy(lastSessionStartedAt = time) }

    override suspend fun setMaterialYouEnabled(enabled: Boolean) = update { it.copy(materialYouEnabled = enabled) }

    private suspend fun update(transform: (AppState) -> AppState) = withContext(dispatchers.io) {
        val next = transform(currentState())
        appStateDao.upsert(next.toEntity())
    }

    private val defaultState = AppState(
        activeBrokerId = null,
        connectionMode = ConnectionMode.VISIBLE_ONLY,
        globalMuteUntil = null,
        lastSessionStartedAt = null,
        materialYouEnabled = true
    )
}
