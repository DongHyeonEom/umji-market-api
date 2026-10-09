package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Organization 가입 요청 항목")
data class OrganizationJoinRequestResponse(
    @field:Schema(description = "가입 요청 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val organizationId: UUID,

    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true)
    val organizationName: String,

    @field:Schema(description = "가입을 요청한 사용자 이름", example = "홍길동", type = "string", required = true)
    val requesterName: String,

    @field:Schema(description = "가입을 요청한 사용자 휴대폰 번호", example = "01012345678", type = "string", required = true)
    val requesterPhone: String,

    @field:Schema(description = "가입 요청 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val requestedAt: Instant,
)