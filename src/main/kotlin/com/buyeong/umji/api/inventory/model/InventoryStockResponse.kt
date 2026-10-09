package com.buyeong.umji.api.inventory.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "InventoryStockResponse API 데이터 모델")
data class InventoryStockResponse(
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val skuId: UUID,

    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,

    @field:Schema(description = "On Hand Quantity 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val onHandQuantity: Int,

    @field:Schema(description = "Reserved Quantity 정보", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val reservedQuantity: Int,

    @field:Schema(description = "주문 가능한 재고 수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val availableQuantity: Int,

    @field:Schema(
        description = "Safety Stock Quantity 정보",
        example = "1",
        format = "int32",
        type = "integer",
        required = true,
        implementation = Int::class,
    )
    val safetyStockQuantity: Int,

    @field:Schema(description = "도매 판매 단위당 기준 SKU 입수 수량", example = "12", type = "integer", required = true, implementation = Int::class)
    val unitsPerBox: Int = 1,

    @field:Schema(description = "완박스 수. 기준 수량을 unitsPerBox로 나눈 몫", example = "8", type = "integer", required = true, implementation = Int::class)
    val onHandBoxCount: Int = 0,

    @field:Schema(description = "완박스 환산 후 남은 낱개 재고", example = "3", type = "integer", required = true, implementation = Int::class)
    val onHandRemainderQuantity: Int = 0,

    @field:Schema(description = "예약 재고의 완박스 수", example = "2", type = "integer", required = true, implementation = Int::class)
    val reservedBoxCount: Int = 0,

    @field:Schema(description = "예약 재고의 낱개 잔량", example = "1", type = "integer", required = true, implementation = Int::class)
    val reservedRemainderQuantity: Int = 0,

    @field:Schema(description = "가용 재고의 완박스 수", example = "6", type = "integer", required = true, implementation = Int::class)
    val availableBoxCount: Int = 0,

    @field:Schema(description = "가용 재고의 낱개 잔량", example = "2", type = "integer", required = true, implementation = Int::class)
    val availableRemainderQuantity: Int = 0,
)