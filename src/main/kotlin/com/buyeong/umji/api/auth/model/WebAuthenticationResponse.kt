package com.buyeong.umji.api.auth.model

import com.buyeong.umji.api.auth.model.WebLoginResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "웹 로그인 인증 token과 계정 요약")
data class WebLoginResponse(
    @field:Schema(description = "발급된 인증 token", required = true) val tokens: TokenPairResponse,
    @field:Schema(description = "인증된 계정", required = true) val account: AuthenticatedAccountResponse,
) {
    companion object {
        fun from(result: WebLoginResult) = WebLoginResponse(
            TokenPairResponse(result.tokens.accessToken, result.tokens.accessTokenExpiresAt, result.tokens.refreshToken),
            AuthenticatedAccountResponse(result.account.id, result.account.name, result.account.status),
        )
    }
}

@Schema(description = "관리자 TOTP 등록용 secret과 URI")
data class TotpSetupResponse(
    @field:Schema(description = "Authenticator 앱에 한 번만 표시되는 TOTP secret", example = "JBSWY3DPEHPK3PXP", required = true) val secret: String,
    @field:Schema(description = "Authenticator 앱으로 등록할 otpauth URI", example = "otpauth://totp/Umji%20Market:admin", required = true) val provisioningUri: String,
)