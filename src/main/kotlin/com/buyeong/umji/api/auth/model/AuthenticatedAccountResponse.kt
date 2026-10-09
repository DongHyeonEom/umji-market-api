package com.buyeong.umji.api.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "AuthenticatedAccountResponse API 데이터 모델")
data class AuthenticatedAccountResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,
)