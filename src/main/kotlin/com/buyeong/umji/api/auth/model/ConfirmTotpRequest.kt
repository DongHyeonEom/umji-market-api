package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(description = "관리자 TOTP 등록 확인 요청")
data class ConfirmTotpRequest(
    @field:Pattern(regexp = "^\\d{6}$")
    @field:Schema(description = "Authenticator 앱에 표시된 6자리 코드", example = "123456", type = "string", required = true)
    val code: String,
)