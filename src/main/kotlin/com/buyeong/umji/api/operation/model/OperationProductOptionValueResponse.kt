package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationProductOptionValueResponse API 데이터 모델")
data class OperationProductOptionValueResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "값", example = "예시 값", type = "string", required = true)
    val value: String,

    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val displayOrder: Int,
)