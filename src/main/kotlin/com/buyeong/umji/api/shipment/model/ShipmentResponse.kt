package com.buyeong.umji.api.shipment.model

import com.buyeong.umji.api.shipment.model.ShipmentChange
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "ShipmentResponse API 데이터 모델")
data class ShipmentResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val orderId: UUID,
    @field:Schema(description = "배송 상태 코드", example = "PREPARING", type = "string", required = true) val shippingStatus: String,
    @field:Schema(description = "택배사 코드", example = "예시 값", type = "string", required = true) val carrierCode: String?,
    @field:Schema(description = "택배 송장 번호", example = "예시 값", type = "string", required = true) val trackingNumber: String?,
)

fun ShipmentChange.toResponse() = ShipmentResponse(orderId, status, carrierCode, trackingNumber)