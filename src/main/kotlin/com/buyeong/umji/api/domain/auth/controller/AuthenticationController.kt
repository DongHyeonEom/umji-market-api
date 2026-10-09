package com.buyeong.umji.api.domain.auth.controller

import com.buyeong.umji.api.domain.auth.dto.AuthenticationStatus
import com.buyeong.umji.api.domain.auth.dto.IssuedTokensDto
import com.buyeong.umji.api.domain.auth.dto.LoginResultDto
import com.buyeong.umji.api.domain.auth.dto.PhoneLoginCommandDto
import com.buyeong.umji.api.domain.auth.dto.RefreshTokenCommandDto
import com.buyeong.umji.api.domain.auth.dto.RevokeRefreshTokenCommandDto
import com.buyeong.umji.api.domain.auth.dto.WebLoginCommandDto
import com.buyeong.umji.api.domain.auth.dto.WebPasswordCommandDto
import com.buyeong.umji.api.domain.auth.model.AuthenticatedAccountResponse
import com.buyeong.umji.api.domain.auth.model.ConfirmTotpRequest
import com.buyeong.umji.api.domain.auth.model.LoginStatus
import com.buyeong.umji.api.domain.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.domain.auth.model.PhoneLoginResponse
import com.buyeong.umji.api.domain.auth.model.RefreshTokenRequest
import com.buyeong.umji.api.domain.auth.model.RevokeRefreshTokenRequest
import com.buyeong.umji.api.domain.auth.model.SetWebPasswordRequest
import com.buyeong.umji.api.domain.auth.model.TokenPairResponse
import com.buyeong.umji.api.domain.auth.model.TotpSetupResponse
import com.buyeong.umji.api.domain.auth.model.WebLoginRequest
import com.buyeong.umji.api.domain.auth.model.WebLoginResponse
import com.buyeong.umji.api.domain.auth.service.AuthenticationService
import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.auth.service.WebAuthenticationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
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
    private val authenticationService: AuthenticationService,
    private val webAuthentication: WebAuthenticationService,
    private val currentAccounts: CurrentAccountService,
) {
    @Operation(summary = "휴대폰 번호로 로그인", description = "휴대폰 번호를 확인하고 로그인 결과와 인증 token을 반환")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: PhoneLoginRequest) =
        authenticationService.login(PhoneLoginCommandDto(request.phone, request.deviceId)).toResponse()

    @Operation(summary = "웹 전용 비밀번호 설정", description = "휴대폰 로그인으로 인증된 활성 계정에 웹 전용 비밀번호를 설정하고 기존 세션을 폐기")
    @PostMapping("/web-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setWebPassword(@Valid @RequestBody request: SetWebPasswordRequest) =
        webAuthentication.setPassword(WebPasswordCommandDto(currentAccounts.activeAccountPublicId(), request.password))

    @Operation(summary = "웹 비밀번호 로그인", description = "휴대폰 번호와 웹 비밀번호를 검증하고 관리자에게 TOTP 2차 인증을 요구")
    @PostMapping("/web/login")
    fun webLogin(@Valid @RequestBody request: WebLoginRequest, servletRequest: HttpServletRequest) = WebLoginResponse.from(
        webAuthentication.login(WebLoginCommandDto(request.phone, request.password, request.totpCode, request.deviceId, servletRequest.remoteAddr ?: "unknown")),
    )

    @Operation(summary = "관리자 TOTP 등록 시작", description = "상위 관리자의 Google Authenticator 호환 TOTP secret과 등록 URI를 반환")
    @PostMapping("/admin/totp/setup")
    fun setupTotp() = webAuthentication.setupTotp(currentAccounts.activeAccountPublicId()).let { TotpSetupResponse(it.secret, it.provisioningUri) }

    @Operation(summary = "관리자 TOTP 등록 확인", description = "Authenticator 앱의 유효 코드를 검증하고 2차 인증을 활성화")
    @PostMapping("/admin/totp/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun confirmTotp(@Valid @RequestBody request: ConfirmTotpRequest) =
        webAuthentication.confirmTotp(currentAccounts.activeAccountPublicId(), request.code)

    @Operation(summary = "인증 token 갱신", description = "유효한 refresh token을 검증하고 새 token 쌍을 발급")
    @PostMapping("/token/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest) =
        authenticationService.refresh(RefreshTokenCommandDto(request.refreshToken, request.deviceId)).toResponse()

    @Operation(summary = "refresh token 폐기", description = "refresh token 세션을 폐기하고 본문 없는 성공 응답을 반환")
    @PostMapping("/tokens/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@Valid @RequestBody request: RevokeRefreshTokenRequest) {
        authenticationService.revoke(RevokeRefreshTokenCommandDto(request.refreshToken))
    }

    private fun LoginResultDto.toResponse() = PhoneLoginResponse(
        if (status == AuthenticationStatus.AUTHENTICATED) LoginStatus.AUTHENTICATED else LoginStatus.PHONE_VERIFICATION_REQUIRED,
        account?.let { AuthenticatedAccountResponse(it.id, it.name, it.status) },
        tokens?.toResponse(),
    )

    private fun IssuedTokensDto.toResponse() = TokenPairResponse(accessToken, accessTokenExpiresAt, refreshToken)
}