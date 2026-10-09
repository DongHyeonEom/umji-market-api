package com.buyeong.umji.api.domain.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "웹 비밀번호 설정 요청")
data class SetWebPasswordRequest(
    @field:NotBlank
    @field:Size(min = 15, max = 128,)
    @field:Schema(description = "새 웹 전용 비밀번호", example = "correct horse battery staple", type = "string", required = true)
    val password: String,
)