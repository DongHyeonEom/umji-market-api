package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "판매 채널 상품 listing 수정 요청")
data class UpdateChannelListingRequest(
    @field:NotNull
    @field:Schema(description = "대상 채널 카테고리 UUID", format = "uuid", type = "string", required = true)
    val categoryId: UUID,

    @field:NotBlank
    @field:Schema(description = "노출 상태: DISPLAYED 또는 HIDDEN", example = "DISPLAYED", type = "string", required = true)
    val displayStatus: String,

    @field:Min(0)
    @field:Schema(description = "채널 내 상품 표시 순서", example = "0", type = "integer", required = true)
    val displayOrder: Int,
)