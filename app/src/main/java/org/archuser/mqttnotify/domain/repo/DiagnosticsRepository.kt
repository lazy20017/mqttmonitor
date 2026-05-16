package org.archuser.mqttnotify.domain.repo

import kotlinx.coroutines.flow.Flow

interface DiagnosticsRepository {
    fun observeEvents(): Flow<List<String>>
    suspend fun log(event: String)
}
