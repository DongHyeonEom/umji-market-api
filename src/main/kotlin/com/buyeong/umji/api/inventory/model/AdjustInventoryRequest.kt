package com.buyeong.umji.api.inventory.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "AdjustInventoryRequest API 데이터 모델")
data class AdjustInventoryRequest(
    @field:NotNull @field:Schema(
        description = "Quantity Delta 정보",
        example = "1",
        format = "int32",
        type = "integer",
        required = true,
        implementation = Int::class,
    ) val quantityDelta: Int,
    @field:NotBlank @field:Size(max = 30) @field:Schema(description = "변경 또는 요청 사유", example = "예시 값", type = "string", required = true) val reason: String,
    @field:Size(max = 500) @field:Schema(description = "Memo 정보", example = "예시 값", type = "string", required = false) val memo: String? = null,
    @field:Schema(
        description = "Safety Stock Quantity 정보",
        example = "1",
        format = "int32",
        type = "integer",
        required = false,
        implementation = Int::class,
    ) val safetyStockQuantity: Int? = null,
)