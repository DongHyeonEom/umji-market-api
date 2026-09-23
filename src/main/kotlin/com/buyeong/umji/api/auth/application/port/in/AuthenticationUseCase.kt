package com.buyeong.umji.api.auth.application.port.`in`

import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.model.RefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.RevokeRefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.LoginResult

interface AuthenticationUseCase {
    fun login(command: PhoneLoginCommand): LoginResult
    fun refresh(command: RefreshTokenCommand): IssuedTokens
    fun revoke(command: RevokeRefreshTokenCommand)
}
