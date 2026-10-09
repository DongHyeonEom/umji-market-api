package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Organization 구성원 초대 요청")
data class InviteOrganizationMemberRequest(
    @field:NotBlank
    @field:Schema(description = "초대할 사용자의 휴대폰 번호", example = "01012345678", type = "string", required = true)
    val phone: String,
)