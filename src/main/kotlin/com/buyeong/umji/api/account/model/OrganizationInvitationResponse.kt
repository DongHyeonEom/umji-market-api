package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "사용자에게 전달된 Organization 초대 정보")
data class OrganizationInvitationResponse(
    @field:Schema(description = "초대 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "Organization 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val organizationId: UUID,

    @field:Schema(description = "Organization 표시 이름", example = "엄지상회", type = "string", required = true)
    val organizationName: String,

    @field:Schema(description = "초대 대상 휴대폰 번호", example = "01012345678", type = "string", required = true)
    val invitedPhone: String,
)