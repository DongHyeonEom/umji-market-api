package com.buyeong.umji.api.sales.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "구매 Organization 영업 담당자 배정 요청")
data class SalesAssignmentRequest(
    @field:Schema(description = "담당 영업 계정 공개 식별자(UUID). 활성 SALES_MANAGER 계정만 배정 가능", example = "00000000-0000-0000-0000-000000000001", type = "string", required = true)
    val salesAccountId: UUID,

    @field:Schema(description = "선택 인센티브율(basis points). null은 인센티브 미설정", example = "30", type = "integer", required = false, nullable = true)
    @field:Min(1)
    @field:Max(10_000)
    val commissionRateBps: Int?,

    @field:Schema(description = "배정 또는 변경 사유 코드", example = "INITIAL_ASSIGNMENT", type = "string", required = true)
    @field:NotBlank
    @field:Size(max = 30,)
    val assignmentReason: String,
)