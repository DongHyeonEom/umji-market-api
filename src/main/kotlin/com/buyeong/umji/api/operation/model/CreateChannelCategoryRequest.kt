package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "판매 채널 전용 카테고리 등록 요청")
data class CreateChannelCategoryRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "채널 카테고리명", example = "공구", type = "string", required = true)
    val name: String,

    @field:Schema(description = "같은 채널의 상위 카테고리 UUID", format = "uuid", type = "string", required = false)
    val parentId: UUID? = null,

    @field:Min(0)
    @field:Schema(description = "채널 내 표시 순서", example = "0", type = "integer", required = false)
    val displayOrder: Int = 0,

    @field:NotBlank
    @field:Schema(description = "노출 상태: DISPLAYED 또는 HIDDEN", example = "HIDDEN", type = "string", required = true)
    val displayStatus: String = "HIDDEN",
)