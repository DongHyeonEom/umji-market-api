package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "PhoneLoginResponse API 데이터 모델")
data class PhoneLoginResponse(
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "object", required = true) val status: LoginStatus,
    @field:Schema(description = "Account 정보", example = "예시 값", type = "object", required = true) val account: AuthenticatedAccountResponse? = null,
    @field:Schema(description = "Tokens 정보", example = "예시 값", type = "object", required = true) val tokens: TokenPairResponse? = null,
)

enum class LoginStatus {
    AUTHENTICATED,
    PHONE_VERIFICATION_REQUIRED,
}

@Schema(description = "AuthenticatedAccountResponse API 데이터 모델")
data class AuthenticatedAccountResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
)

@Schema(description = "TokenPairResponse API 데이터 모델")
data class TokenPairResponse(
    @field:Schema(description = "API 인증 access token", example = "예시 값", type = "string", required = true) val accessToken: String,
    @field:Schema(description = "access token 만료 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val accessTokenExpiresAt:
    Instant,
    @field:Schema(description = "갱신 또는 폐기할 refresh token", example = "예시 값", type = "string", required = true) val refreshToken: String,
)
