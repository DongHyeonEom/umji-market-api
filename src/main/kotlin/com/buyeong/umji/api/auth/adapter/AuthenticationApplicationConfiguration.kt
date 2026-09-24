package com.buyeong.umji.api.auth.adapter

import com.buyeong.umji.api.auth.application.AuthenticationService
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthenticationApplicationConfiguration {
    @Bean
    fun authenticationService(
        accounts: AccountAuthenticationPort,
        sessions: RefreshSessionPort,
        tokens: AccessTokenIssuerPort,
    ) = AuthenticationService(accounts, sessions, tokens)
}