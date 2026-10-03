package com.buyeong.umji.api.inventory.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "InventoryStockResponse API 데이터 모델")
data class InventoryStockResponse(
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val skuId: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "On Hand Quantity 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val onHandQuantity: Int,
    @field:Schema(description = "Reserved Quantity 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val reservedQuantity: Int,
    @field:Schema(description = "주문 가능한 재고 수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val availableQuantity: Int,
    @field:Schema(
        description = "Safety Stock Quantity 정보",
        example = "1",
        format = "int32",
        type = "integer",
        required = true,
        implementation = Int::class,
    ) val safetyStockQuantity: Int,
)

@Schema(description = "InventoryMovementResponse API 데이터 모델")
data class InventoryMovementResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val id: Long,
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val skuId: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "재고 변동 유형 코드", example = "예시 값", type = "string", required = true) val movementType: String,
    @field:Schema(description = "Quantity Delta 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val quantityDelta: Int,
    @field:Schema(description = "Reference Type 정보", example = "예시 값", type = "string", required = true) val referenceType: String?,
    @field:Schema(description = "Reference Id 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val referenceId: UUID?,
    @field:Schema(description = "Memo 정보", example = "예시 값", type = "string", required = true) val memo: String?,
    @field:Schema(description = "Occurred At 정보", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val occurredAt: Instant,
)

@Schema(description = "재고 변동 이력 검색 결과와 페이지 정보")
data class InventoryMovementPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = InventoryMovementResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<InventoryMovementResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val totalPages: Int,
)