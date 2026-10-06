package com.buyeong.umji.api.auth.model

import java.util.UUID

data class WebAccountCredentials(
    val account: AccountRecord,
    val passwordHash: String?,
    val roles: Set<String>,
    val totpSecret: String? = null,
    val totpEnabled: Boolean = false,
)

data class WebLoginCommand(val phone: String, val password: String, val totpCode: String?, val deviceId: String?, val remoteAddress: String)
data class WebLoginResult(val tokens: IssuedTokens, val account: AuthenticatedAccount)
data class WebPasswordCommand(val accountId: UUID, val password: String)
data class TotpSetupResult(val secret: String, val provisioningUri: String)
