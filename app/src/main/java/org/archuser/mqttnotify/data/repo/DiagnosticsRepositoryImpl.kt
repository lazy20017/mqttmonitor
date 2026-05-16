package org.archuser.mqttnotify.data.repo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository

@Singleton
class DiagnosticsRepositoryImpl @Inject constructor() : DiagnosticsRepository {

    private val events = MutableStateFlow<List<String>>(emptyList())
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun observeEvents(): Flow<List<String>> = events.asStateFlow()

    override suspend fun log(event: String) {
        val timestamped = "${formatter.format(Date())}  $event"
        val next = (events.value + timestamped).takeLast(MAX_EVENTS)
        events.value = next
    }

    private companion object {
        private const val MAX_EVENTS = 300
    }
}
