package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "refresh token 세션 폐기 요청")
data class RevokeRefreshTokenRequest(
    @field:NotBlank @field:Size(max = 512,)
    @field:Schema(description = "갱신 또는 폐기할 refresh token", example = "예시 값", type = "string", required = true)
    val refreshToken: String,
)