package com.buyeong.umji.api.shipment.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import com.buyeong.umji.api.shipment.model.ShipmentResponse
import com.buyeong.umji.api.shipment.model.toResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders/{orderId}/shipment")
class OrderShipmentTrackingController(
    private val currentAccounts: CurrentAccountPort,
    private val shipments: TransactionalShipmentUseCase,
) {
    @PostMapping("/refresh")
    fun refresh(@PathVariable orderId: UUID): ShipmentResponse = shipments.refreshForCustomer(
        orderId,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()
}