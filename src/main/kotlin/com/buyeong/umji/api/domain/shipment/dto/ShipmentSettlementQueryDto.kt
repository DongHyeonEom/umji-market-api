package com.buyeong.umji.api.domain.shipment.dto

import java.time.Instant
import java.time.YearMonth
import java.util.UUID

data class ShipmentSettlementQueryDto(
    val brandId: UUID,
    val month: YearMonth,
    val from: Instant,
    val until: Instant,
    val page: Int,
    val size: Int,
)