package com.buyeong.umji.api.auth.dto

data class TotpSetupResultDto(val secret: String, val provisioningUri: String)