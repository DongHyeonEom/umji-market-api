package com.buyeong.umji.api.domain.auth.dto

data class TotpSetupResultDto(val secret: String, val provisioningUri: String)