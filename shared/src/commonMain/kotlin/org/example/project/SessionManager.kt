package org.example.project

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.models.AuthResponse

object SessionManager {

    private val settings = Settings()

    private const val AUTH_KEY = "auth_data"

    fun saveAuth(auth: AuthResponse) {
        settings.putString(
            AUTH_KEY,
            Json.encodeToString(auth)
        )
    }

    fun getAuth(): AuthResponse? {
        val json = settings.getStringOrNull(AUTH_KEY) ?: return null

        return try {
            Json.decodeFromString<AuthResponse>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearAuth() {
        settings.remove(AUTH_KEY)
    }
}