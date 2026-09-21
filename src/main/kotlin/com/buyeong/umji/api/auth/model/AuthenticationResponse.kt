package com.buyeong.umji.api.auth.model

import java.time.Instant
import java.util.UUID

data class PhoneLoginResponse(
    val status: LoginStatus,
    val account: AuthenticatedAccountResponse? = null,
    val tokens: TokenPairResponse? = null,
)

enum class LoginStatus {
    AUTHENTICATED,
    PHONE_VERIFICATION_REQUIRED,
}

data class AuthenticatedAccountResponse(
    val id: UUID,
    val name: String,
    val status: String,
)

data class TokenPairResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
)
