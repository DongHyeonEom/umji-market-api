package com.buyeong.umji.api.auth.application.model

import java.time.Instant
import java.util.UUID

data class AccountRecord(
    val id: UUID,
    val name: String,
    val status: String,
    val tokenVersion: Long,
    val permissions: Set<String> = emptySet(),
)
enum class AuthenticationStatus { AUTHENTICATED, PHONE_VERIFICATION_REQUIRED }
data class AuthenticatedAccount(val id: UUID, val name: String, val status: String)
data class IssuedTokens(val accessToken: String, val accessTokenExpiresAt: Instant, val refreshToken: String)
data class LoginResult(val status: AuthenticationStatus, val account: AuthenticatedAccount? = null, val tokens: IssuedTokens? = null)
data class RefreshSessionRecord(
    val tokenHash: ByteArray,
    val account: AccountRecord,
    val deviceId: String?,
    val revokedAt: Instant?,
    val lastUsedAt: Instant?,
    val expiresAt: Instant?,
)