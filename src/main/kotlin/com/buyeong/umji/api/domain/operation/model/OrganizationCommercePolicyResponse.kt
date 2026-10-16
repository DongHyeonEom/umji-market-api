package com.buyeong.umji.api.domain.operation.model

import com.buyeong.umji.api.domain.operation.account.dto.OrganizationCommercePolicyDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Organization 구매 정책 응답")
data class OrganizationCommercePolicyResponse(
    @field:Schema(description = "Organization 공개 식별자", format = "uuid", required = true)
    val organizationId: UUID,
    @field:Schema(description = "구매 유형", example = "GENERAL_BUSINESS", required = true)
    val buyerType: String,
    @field:Schema(description = "전액 입금 전 배송 준비 제한 여부", example = "false", required = true)
    val paymentBeforeShipmentRequired: Boolean,
)

fun OrganizationCommercePolicyDto.toResponse() =
    OrganizationCommercePolicyResponse(organizationId, buyerType, paymentBeforeShipmentRequired)
