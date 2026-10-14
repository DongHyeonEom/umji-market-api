package com.buyeong.umji.api.domain.shipment.dto

import java.time.YearMonth
import java.util.UUID

data class ShipmentSettlementDto(
    val brandId: UUID,
    val brandName: String,
    val month: YearMonth,
    val packageCount: Long,
    val actualCostTotal: Long,
    val pricedPackageCount: Long,
    val missingCostCount: Long,
    val carriers: List<ShipmentSettlementCarrierDto>,
    val items: List<ShipmentSettlementPackageDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)