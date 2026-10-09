package com.buyeong.umji.api.auth.dto

data class WebAccountCredentialsDto(
    val account: AccountRecordDto,
    val passwordHash: String?,
    val roles: Set<String>,
    val totpSecret: String? = null,
    val totpEnabled: Boolean = false,
)