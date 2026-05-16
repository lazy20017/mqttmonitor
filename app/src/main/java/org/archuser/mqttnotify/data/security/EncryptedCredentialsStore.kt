package org.archuser.mqttnotify.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EncryptedCredentialsStore @Inject constructor(
    @ApplicationContext context: Context
) : CredentialsStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "broker_credentials",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun savePassword(alias: String, password: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(alias, password).apply()
    }

    override suspend fun getPassword(alias: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(alias, null)
    }

    override suspend fun clearPassword(alias: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(alias).apply()
    }
}
