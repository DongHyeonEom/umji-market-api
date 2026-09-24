package com.buyeong.umji.api.auth.application.model

data class PhoneLoginCommand(val phone: String, val deviceId: String?)
data class RefreshTokenCommand(val refreshToken: String, val deviceId: String?)
data class RevokeRefreshTokenCommand(val refreshToken: String)