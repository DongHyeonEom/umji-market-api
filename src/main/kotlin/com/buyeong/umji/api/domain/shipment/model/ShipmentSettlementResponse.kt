package com.buyeong.umji.api.domain.shipment.model

import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementDto
import java.time.YearMonth
import java.util.UUID

data class ShipmentSettlementResponse(
    val brandId: UUID,
    val brandName: String,
    val month: YearMonth,
    val packageCount: Long,
    val actualCostTotal: Long,
    val pricedPackageCount: Long,
    val missingCostCount: Long,
    val carriers: List<ShipmentSettlementCarrierResponse>,
    val items: List<ShipmentSettlementPackageResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun ShipmentSettlementDto.toResponse() = ShipmentSettlementResponse(
    brandId,
    brandName,
    month,
    packageCount,
    actualCostTotal,
    pricedPackageCount,
    missingCostCount,
    carriers.map { ShipmentSettlementCarrierResponse(it.carrierCode, it.packageCount, it.actualCostTotal, it.missingCostCount) },
    items.map { pkg ->
        ShipmentSettlementPackageResponse(
            pkg.packageId,
            pkg.orderId,
            pkg.orderNumber,
            pkg.orderedAt,
            pkg.registeredAt,
            pkg.shippingStatus,
            pkg.carrierCode,
            pkg.trackingNumber,
            pkg.actualShippingCost,
            pkg.items.map { ShipmentSettlementItemResponse(it.orderItemId, it.skuName, it.boxQuantity, it.amountPerBox, it.lineShippingCost) },
        )
    },
    page,
    size,
    totalElements,
    totalPages,
)