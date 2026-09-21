package com.buyeong.umji.api.auth.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PhoneLoginRequest(
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:Size(max = 100) val deviceId: String? = null,
)

data class RefreshTokenRequest(
    @field:NotBlank @field:Size(max = 512) val refreshToken: String,
    @field:Size(max = 100) val deviceId: String? = null,
)

data class RevokeRefreshTokenRequest(
    @field:NotBlank @field:Size(max = 512) val refreshToken: String,
)
