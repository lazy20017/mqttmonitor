package org.archuser.mqttnotify.data.security

interface CredentialsStore {
    suspend fun savePassword(alias: String, password: String)
    suspend fun getPassword(alias: String): String?
    suspend fun clearPassword(alias: String)
}
