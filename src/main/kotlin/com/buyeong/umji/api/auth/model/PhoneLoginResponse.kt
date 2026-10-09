package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "PhoneLoginResponse API 데이터 모델")
data class PhoneLoginResponse(
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "object", required = true)
    val status: LoginStatus,

    @field:Schema(description = "Account 정보", example = "예시 값", type = "object", required = true)
    val account: AuthenticatedAccountResponse? = null,

    @field:Schema(description = "Tokens 정보", example = "예시 값", type = "object", required = true)
    val tokens: TokenPairResponse? = null,
)

enum class LoginStatus {
    AUTHENTICATED,
    PHONE_VERIFICATION_REQUIRED,
}