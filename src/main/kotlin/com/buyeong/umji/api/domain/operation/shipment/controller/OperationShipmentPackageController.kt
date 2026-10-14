package com.buyeong.umji.api.domain.operation.shipment.controller

import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageResponse
import com.buyeong.umji.api.domain.shipment.model.UpdateShipmentPackageCostRequest
import com.buyeong.umji.api.domain.shipment.model.toResponse
import com.buyeong.umji.api.domain.shipment.service.ShipmentService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/shipping/packages")
class OperationShipmentPackageController(
    private val currentAccounts: CurrentAccountService,
    private val shipments: ShipmentService,
) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_READ')")
    fun packages(): List<ShipmentPackageResponse> =
        shipments.operationPackages(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @PutMapping("/{packageId}/cost")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_WRITE')")
    fun updateCost(
        @PathVariable packageId: UUID,
        @Valid @RequestBody request: UpdateShipmentPackageCostRequest,
    ): ShipmentPackageResponse = shipments.updateActualCost(
        packageId,
        request.actualShippingCost,
        currentAccounts.activeAccountPublicId(),
        request.reason,
    ).toResponse()
}