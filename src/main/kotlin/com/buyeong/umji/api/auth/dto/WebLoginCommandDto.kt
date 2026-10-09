package com.buyeong.umji.api.auth.dto

data class WebLoginCommandDto(val phone: String, val password: String, val totpCode: String?, val deviceId: String?, val remoteAddress: String)