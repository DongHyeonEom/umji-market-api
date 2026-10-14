package com.buyeong.umji.api.domain.shipment.model

data class ShipmentSettlementCarrierResponse(
    val carrierCode: String,
    val packageCount: Long,
    val actualCostTotal: Long,
    val missingCostCount: Long,
)