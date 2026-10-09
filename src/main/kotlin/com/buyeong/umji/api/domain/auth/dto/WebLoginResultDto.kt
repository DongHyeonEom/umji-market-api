package com.buyeong.umji.api.domain.auth.dto

data class WebLoginResultDto(val tokens: IssuedTokensDto, val account: AuthenticatedAccountDto)