package com.buyeong.umji.api.auth.adapter.`in`.web

import com.buyeong.umji.api.auth.application.model.AuthenticationStatus
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.LoginResult
import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.model.RefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.RevokeRefreshTokenCommand
import com.buyeong.umji.api.auth.application.port.`in`.AuthenticationUseCase
import com.buyeong.umji.api.auth.model.AuthenticatedAccountResponse
import com.buyeong.umji.api.auth.model.LoginStatus
import com.buyeong.umji.api.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.auth.model.PhoneLoginResponse
import com.buyeong.umji.api.auth.model.RefreshTokenRequest
import com.buyeong.umji.api.auth.model.RevokeRefreshTokenRequest
import com.buyeong.umji.api.auth.model.TokenPairResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "휴대폰 번호 로그인과 인증 token 발급·갱신·폐기 API")
class AuthenticationController(
    private val authenticationService: AuthenticationUseCase,
) {
    @Operation(summary = "휴대폰 번호로 로그인", description = "휴대폰 번호를 확인하고 로그인 결과와 인증 token을 반환")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: PhoneLoginRequest) =
        authenticationService.login(PhoneLoginCommand(request.phone, request.deviceId)).toResponse()

    @Operation(summary = "인증 token 갱신", description = "유효한 refresh token을 검증하고 새 token 쌍을 발급")
    @PostMapping("/token/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest) =
        authenticationService.refresh(RefreshTokenCommand(request.refreshToken, request.deviceId)).toResponse()

    @Operation(summary = "refresh token 폐기", description = "refresh token 세션을 폐기하고 본문 없는 성공 응답을 반환")
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