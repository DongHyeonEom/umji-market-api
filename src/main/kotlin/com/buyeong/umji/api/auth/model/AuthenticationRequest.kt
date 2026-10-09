package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "휴대폰 번호와 선택적 기기 식별자를 전달하는 로그인 요청")
data class PhoneLoginRequest(
    @field:NotBlank @field:Size(max = 30)
    @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true)
    val phone: String,

    @field:Size(max = 100)
    @field:Schema(description = "로그인 기기 식별자", example = "예시 값", type = "string", required = false)
    val deviceId: String? = null,
)

@Schema(description = "refresh token을 사용한 인증 토큰 갱신 요청")
data class RefreshTokenRequest(
    @field:NotBlank @field:Size(max = 512)
    @field:Schema(description = "갱신 또는 폐기할 refresh token", example = "예시 값", type = "string", required = true)
    val refreshToken: String,

    @field:Size(max = 100)
    @field:Schema(description = "로그인 기기 식별자", example = "예시 값", type = "string", required = false)
    val deviceId: String? = null,
)

@Schema(description = "refresh token 세션 폐기 요청")
data class RevokeRefreshTokenRequest(
    @field:NotBlank @field:Size(max = 512)
    @field:Schema(description = "갱신 또는 폐기할 refresh token", example = "예시 값", type = "string", required = true)
    val refreshToken: String,
)
