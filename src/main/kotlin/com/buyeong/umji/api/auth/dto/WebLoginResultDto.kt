package com.buyeong.umji.api.auth.dto

data class WebLoginResultDto(val tokens: IssuedTokensDto, val account: AuthenticatedAccountDto)