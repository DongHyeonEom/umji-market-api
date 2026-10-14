package com.buyeong.umji.api.domain.shipment.dto

data class ShipmentSettlementCarrierDto(
    val carrierCode: String,
    val packageCount: Long,
    val actualCostTotal: Long,
    val missingCostCount: Long,
)