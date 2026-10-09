package com.buyeong.umji.api.domain.auth.dto

import java.time.Instant

data class IssuedTokensDto(val accessToken: String, val accessTokenExpiresAt: Instant, val refreshToken: String)