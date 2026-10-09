package com.buyeong.umji.api.domain.shipment.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "ShipmentTrackingRequest API 데이터 모델")
data class ShipmentTrackingRequest(
    @field:NotBlank
    @field:Size(max = 80,)
    @field:Schema(description = "택배사 코드", example = "예시 값", type = "string", required = true)
    val carrierCode: String,

    @field:NotBlank
    @field:Size(max = 100,)
    @field:Schema(description = "택배 송장 번호", example = "예시 값", type = "string", required = true)
    val trackingNumber: String,
)