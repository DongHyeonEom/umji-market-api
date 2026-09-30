package com.buyeong.umji.api.shipment.adapter

import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ShipmentTrackingJob(private val shipments: TransactionalShipmentUseCase) {
    @Scheduled(cron = "\${shipment.tracking.poll-cron:0 0 * * * *}", zone = "Asia/Seoul")
    fun synchronizeInTransitOrders(): Int = shipments.synchronizeTrackingStatus()
}