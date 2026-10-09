package com.buyeong.umji.api.domain.auth.dto

data class LoginResultDto(val status: AuthenticationStatus, val account: AuthenticatedAccountDto? = null, val tokens: IssuedTokensDto? = null)