package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "Organization 가입 요청")
data class RequestOrganizationJoinRequest(
    @field:NotNull @field:Schema(
        description = "가입을 요청할 Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val organizationId: UUID,
)