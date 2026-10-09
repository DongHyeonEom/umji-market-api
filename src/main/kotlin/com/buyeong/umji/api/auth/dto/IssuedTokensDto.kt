package com.buyeong.umji.api.auth.dto

import java.time.Instant

data class IssuedTokensDto(val accessToken: String, val accessTokenExpiresAt: Instant, val refreshToken: String)