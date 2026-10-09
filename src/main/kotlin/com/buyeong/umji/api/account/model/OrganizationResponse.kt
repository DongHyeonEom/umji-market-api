package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "현재 계정의 Organization 정보")
data class OrganizationResponse(
    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "Organization 유형 코드", example = "BUSINESS", type = "string", required = true)
    val type: String,

    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true)
    val name: String,

    @field:Schema(description = "현재 계정의 그룹 대표자 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val representative: Boolean,

    @field:Schema(description = "Organization capability 목록", example = "[\"BUYER\"]", required = true)
    val capabilities: Set<String>,
)