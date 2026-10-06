package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "휴대폰 번호와 웹 비밀번호로 인증하는 요청")
data class WebLoginRequest(
    @field:NotBlank @field:Size(max = 30) @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true) val phone: String,
    @field:NotBlank @field:Size(
        min = 15,
        max = 128,
    ) @field:Schema(description = "웹 전용 비밀번호", example = "correct horse battery staple", type = "string", required = true) val password: String,
    @field:Pattern(
        regexp = "^\\d{6}$",
    ) @field:Schema(description = "2차 인증 대상 관리자 계정의 Google Authenticator 호환 TOTP 코드", example = "123456", type = "string", required = false) val totpCode: String? = null,
    @field:Size(max = 100) @field:Schema(description = "로그인 기기 식별자", example = "web-browser", type = "string", required = false) val deviceId: String? = null,
)

@Schema(description = "웹 비밀번호 설정 요청")
data class SetWebPasswordRequest(
    @field:NotBlank @field:Size(
        min = 15,
        max = 128,
    ) @field:Schema(description = "새 웹 전용 비밀번호", example = "correct horse battery staple", type = "string", required = true) val password: String,
)

@Schema(description = "관리자 TOTP 등록 확인 요청")
data class ConfirmTotpRequest(
    @field:Pattern(regexp = "^\\d{6}$") @field:Schema(description = "Authenticator 앱에 표시된 6자리 코드", example = "123456", type = "string", required = true) val code: String,
)
