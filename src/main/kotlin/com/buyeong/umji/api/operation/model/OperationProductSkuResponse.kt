package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationProductSkuResponse API 데이터 모델")
data class OperationProductSkuResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "판매 가격(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val salePrice: Long,

    @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val listPrice: Long?,

    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true)
    val salesStatus: String,

    @field:ArraySchema(
        schema = Schema(implementation = UUID::class),
    ) @field:Schema(description = "SKU에 연결할 상품 옵션 값 공개 식별자(UUID) 목록", example = "[]", type = "array", required = true)
    val optionValueIds: Set<UUID> = emptySet(),
)