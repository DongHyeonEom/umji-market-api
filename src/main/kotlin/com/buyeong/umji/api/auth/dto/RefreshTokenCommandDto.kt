package com.buyeong.umji.api.auth.dto

data class RefreshTokenCommandDto(val refreshToken: String, val deviceId: String?)