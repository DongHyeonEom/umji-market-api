package com.buyeong.umji.api.operation.shipment.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.shipment.model.ShipmentResponse
import com.buyeong.umji.api.shipment.model.ShipmentTrackingRequest
import com.buyeong.umji.api.shipment.model.toResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/orders/{orderId}/shipment")
class OperationShipmentController(
    private val currentAccounts: CurrentAccountPort,
    private val shipments: TransactionalShipmentUseCase,
) {
    @PutMapping("/tracking")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun registerTracking(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: ShipmentTrackingRequest,
    ): ShipmentResponse = shipments.registerTracking(
        orderId,
        request.carrierCode,
        request.trackingNumber,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()

    @PostMapping("/delivered")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun markDelivered(@PathVariable orderId: UUID): ShipmentResponse = shipments.markDelivered(
        orderId,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()
}