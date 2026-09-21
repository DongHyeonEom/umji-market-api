package com.buyeong.umji.api.auth.controller

import com.buyeong.umji.api.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.auth.model.RefreshTokenRequest
import com.buyeong.umji.api.auth.model.RevokeRefreshTokenRequest
import com.buyeong.umji.api.auth.service.AuthenticationService
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
    private val authenticationService: AuthenticationService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: PhoneLoginRequest) = authenticationService.login(request)

    @PostMapping("/token/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest) = authenticationService.refresh(request)

    @PostMapping("/tokens/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@Valid @RequestBody request: RevokeRefreshTokenRequest) {
        authenticationService.revoke(request)
    }
}
