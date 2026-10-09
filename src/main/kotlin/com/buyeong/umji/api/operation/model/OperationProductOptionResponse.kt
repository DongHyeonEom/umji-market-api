package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationProductOptionResponse API 데이터 모델")
data class OperationProductOptionResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val displayOrder: Int,

    @field:ArraySchema(
        schema = Schema(implementation = OperationProductOptionValueResponse::class),
    ) @field:Schema(description = "상품 옵션 값 목록", example = "[]", type = "array", required = true)
    val values: List<OperationProductOptionValueResponse>,
)