package com.buyeong.umji.api.domain.auth.dto

data class RefreshTokenCommandDto(val refreshToken: String, val deviceId: String?)