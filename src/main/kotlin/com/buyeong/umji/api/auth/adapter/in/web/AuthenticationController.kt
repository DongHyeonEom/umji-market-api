package com.buyeong.umji.api.auth.adapter.`in`.web

import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.model.RefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.RevokeRefreshTokenCommand
import com.buyeong.umji.api.auth.application.port.`in`.AuthenticationUseCase
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.LoginResult
import com.buyeong.umji.api.auth.application.model.AuthenticationStatus
import com.buyeong.umji.api.auth.model.AuthenticatedAccountResponse
import com.buyeong.umji.api.auth.model.LoginStatus
import com.buyeong.umji.api.auth.model.PhoneLoginResponse
import com.buyeong.umji.api.auth.model.TokenPairResponse
import com.buyeong.umji.api.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.auth.model.RefreshTokenRequest
import com.buyeong.umji.api.auth.model.RevokeRefreshTokenRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationUseCase,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: PhoneLoginRequest) =
        authenticationService.login(PhoneLoginCommand(request.phone, request.deviceId)).toResponse()

    @PostMapping("/token/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest) =
        authenticationService.refresh(RefreshTokenCommand(request.refreshToken, request.deviceId)).toResponse()

    @PostMapping("/tokens/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@Valid @RequestBody request: RevokeRefreshTokenRequest) {
        authenticationService.revoke(RevokeRefreshTokenCommand(request.refreshToken))
    }

    private fun LoginResult.toResponse() = PhoneLoginResponse(
        if (status == AuthenticationStatus.AUTHENTICATED) LoginStatus.AUTHENTICATED else LoginStatus.PHONE_VERIFICATION_REQUIRED,
        account?.let { AuthenticatedAccountResponse(it.id, it.name, it.status) },
        tokens?.toResponse(),
    )

    private fun IssuedTokens.toResponse() = TokenPairResponse(accessToken, accessTokenExpiresAt, refreshToken)
}
