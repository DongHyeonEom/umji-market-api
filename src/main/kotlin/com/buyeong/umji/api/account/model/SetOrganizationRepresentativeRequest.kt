package com.buyeong.umji.api.account.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "Organization 대표자 지정 요청")
data class SetOrganizationRepresentativeRequest(
    @field:NotNull @field:Schema(
        description = "대표자로 지정할 계정 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    )
    val accountId: UUID,
)