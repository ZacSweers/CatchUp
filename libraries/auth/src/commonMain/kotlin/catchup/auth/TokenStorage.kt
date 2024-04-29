// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package catchup.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import okio.Path

/**
 * A simple [TokenStorage] that uses `DataStore` to store [AuthenticationResponse] for reuse across
 * app sessions.
 */
interface TokenStorage {
  /** Updates the current stored auth data. */
  suspend fun updateAuthData(authData: AuthData)

  /** Returns the current auth data or null if none are stored. */
  suspend fun getAuthData(): AuthData?

  companion object {
    fun create(pathFactory: (prefix: String) -> Path): TokenStorage = TokenStorageImpl(pathFactory)
  }
}

data class AuthData(val tokenType: String, val expiration: Instant, val token: String)

internal class TokenStorageImpl(pathFactory: (prefix: String) -> Path) : TokenStorage {
  private val datastore =
    PreferenceDataStoreFactory.createWithPath { pathFactory(TOKEN_STORAGE_FILE_NAME_PREFIX) }

  override suspend fun updateAuthData(authData: AuthData) {
    datastore.edit { prefs ->
      prefs[expirationKey] = authData.expiration.toEpochMilliseconds()
      prefs[authTokenTypeKey] = authData.tokenType
      prefs[authTokenKey] = authData.token
    }
  }

  override suspend fun getAuthData(): AuthData? {
    val expiration = datastore.data.first()[expirationKey]
    val tokenType = datastore.data.first()[authTokenTypeKey]
    val token = datastore.data.first()[authTokenKey]
    return if (expiration != null && tokenType != null && token != null) {
      AuthData(tokenType, Instant.fromEpochMilliseconds(expiration), token)
    } else {
      null
    }
  }

  companion object {
    private const val TOKEN_STORAGE_FILE_NAME_PREFIX = "TokenManager:"
    val authTokenKey = stringPreferencesKey("AUTH_TOKEN")
    val expirationKey = longPreferencesKey("AUTH_TOKEN_EXPIRATION")
    val authTokenTypeKey = stringPreferencesKey("AUTH_TOKEN_TYPE")
  }
}
