package com.buyeong.umji.api.auth.adapter.`in`.web

import com.buyeong.umji.api.auth.application.AuthenticationService
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.LoginResult
import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.model.RefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.RevokeRefreshTokenCommand
import com.buyeong.umji.api.auth.application.port.`in`.AuthenticationUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionalAuthenticationUseCase(private val authentication: AuthenticationService) : AuthenticationUseCase {
    @Transactional override fun login(command: PhoneLoginCommand): LoginResult = authentication.login(command)

    @Transactional override fun refresh(command: RefreshTokenCommand): IssuedTokens = authentication.refresh(command)

    @Transactional override fun revoke(command: RevokeRefreshTokenCommand) = authentication.revoke(command)
}