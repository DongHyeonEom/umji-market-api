package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "배송지와 세금계산서 설정을 포함한 주문 생성 요청")
data class CreateOrderRequest(
    @field:Schema(description = "저장된 배송지 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val shippingAddressId: UUID,
    @field:Schema(description = "세금계산서 발행 요청 여부", example = "true", type = "boolean", required = false, implementation = Boolean::class)
    val taxInvoiceRequested: Boolean? = null,

    @field:Schema(
        description = "대표자가 Organization 기본 세금계산서 설정을 갱신할지 여부",
        example = "true",
        type = "boolean",
        required = false,
        implementation = Boolean::class,
    )
    val updateDefaultTaxInvoicePreference: Boolean = false,
)