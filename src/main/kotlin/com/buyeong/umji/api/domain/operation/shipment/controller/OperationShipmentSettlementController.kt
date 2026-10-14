package com.buyeong.umji.api.domain.operation.shipment.controller

import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.shipment.model.ShipmentSettlementResponse
import com.buyeong.umji.api.domain.shipment.model.toResponse
import com.buyeong.umji.api.domain.shipment.service.ShipmentSettlementService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/shipping/settlements")
@Validated
class OperationShipmentSettlementController(
    private val currentAccounts: CurrentAccountService,
    private val settlements: ShipmentSettlementService,
) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_READ')")
    fun monthly(
        @RequestParam brandId: UUID,
        @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") month: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ShipmentSettlementResponse = settlements.monthly(
        brandId,
        month,
        currentAccounts.activeAccountPublicId(),
        page,
        size,
    ).toResponse()
}