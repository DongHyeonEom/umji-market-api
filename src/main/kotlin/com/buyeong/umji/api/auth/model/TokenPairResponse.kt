package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "TokenPairResponse API 데이터 모델")
data class TokenPairResponse(
    @field:Schema(description = "API 인증 access token", example = "예시 값", type = "string", required = true)
    val accessToken: String,

    @field:Schema(description = "access token 만료 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val accessTokenExpiresAt: Instant,
    @field:Schema(description = "갱신 또는 폐기할 refresh token", example = "예시 값", type = "string", required = true)
    val refreshToken: String,
)