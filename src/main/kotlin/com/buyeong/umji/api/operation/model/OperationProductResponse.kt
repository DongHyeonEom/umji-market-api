package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "OperationProductResponse API 데이터 모델")
data class OperationProductResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "카테고리 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val categoryId: UUID,

    @field:Schema(description = "브랜드 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val brandId: UUID?,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = true)
    val description: String?,

    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true)
    val displayStatus: String,

    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true)
    val salesStatus: String,

    @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val displayOrder: Int,

    @field:ArraySchema(
        schema = Schema(implementation = OperationProductImageResponse::class),
    ) @field:Schema(description = "상품 이미지 목록", example = "[]", type = "array", required = true)
    val images: List<OperationProductImageResponse> = emptyList(),

    @field:ArraySchema(
        schema = Schema(implementation = OperationProductOptionResponse::class),
    ) @field:Schema(description = "상품 옵션 목록", example = "[]", type = "array", required = true)
    val options: List<OperationProductOptionResponse> = emptyList(),

    @field:ArraySchema(
        schema = Schema(implementation = OperationProductSkuResponse::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true)
    val skus: List<OperationProductSkuResponse> = emptyList(),
)