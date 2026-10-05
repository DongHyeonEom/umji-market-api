package com.buyeong.umji.api.auth.adapter

import com.buyeong.umji.api.auth.application.AuthenticationService
import com.buyeong.umji.api.auth.application.WebAuthenticationService
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.application.port.out.CredentialEncoderPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import com.buyeong.umji.api.auth.application.port.out.TotpPort
import com.buyeong.umji.api.auth.application.port.out.WebCredentialPort
import com.buyeong.umji.api.auth.application.port.out.WebLoginAttemptPort
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

    @Bean
    fun webAuthenticationService(
        credentials: WebCredentialPort,
        encoder: CredentialEncoderPort,
        totp: TotpPort,
        sessions: RefreshSessionPort,
        tokens: AccessTokenIssuerPort,
        attempts: WebLoginAttemptPort,
    ) = WebAuthenticationService(credentials, encoder, totp, sessions, tokens, attempts)
}