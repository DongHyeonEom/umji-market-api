package com.buyeong.umji.api.domain.auth.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 TOTP 등록용 secret과 URI")
data class TotpSetupResponse(
    @field:Schema(description = "Authenticator 앱에 한 번만 표시되는 TOTP secret", example = "JBSWY3DPEHPK3PXP", required = true)
    val secret: String,

    @field:Schema(description = "Authenticator 앱으로 등록할 otpauth URI", example = "otpauth://totp/Umji%20Market:admin", required = true)
    val provisioningUri: String,
)