package com.buyeong.umji.api.domain.account.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Organization 가입 요청 처리 결과")
data class DecideOrganizationJoinRequest(
    @field:NotNull
    @field:Schema(description = "가입 요청 승인 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val approved: Boolean,
)