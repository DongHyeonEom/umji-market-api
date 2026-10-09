package com.buyeong.umji.api.domain.auth.model

import com.buyeong.umji.api.domain.auth.dto.WebLoginResultDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "웹 로그인 인증 token과 계정 요약")
data class WebLoginResponse(
    @field:Schema(description = "발급된 인증 token", required = true)
    val tokens: TokenPairResponse,

    @field:Schema(description = "인증된 계정", required = true)
    val account: AuthenticatedAccountResponse,
) {
    companion object {
        fun from(result: WebLoginResultDto) = WebLoginResponse(
            TokenPairResponse(result.tokens.accessToken, result.tokens.accessTokenExpiresAt, result.tokens.refreshToken),
            AuthenticatedAccountResponse(result.account.id, result.account.name, result.account.status),
        )
    }
}