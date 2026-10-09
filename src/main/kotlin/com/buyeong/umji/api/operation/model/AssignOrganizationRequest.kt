package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "AssignOrganizationRequest 요청 또는 응답 모델")
data class AssignOrganizationRequest(
    @field:NotNull @field:Schema(
        description = "Organization 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val organizationId: UUID,
)