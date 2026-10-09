package com.buyeong.umji.api.operation.shipment.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.shipment.model.ShipmentResponse
import com.buyeong.umji.api.shipment.model.ShipmentTrackingRequest
import com.buyeong.umji.api.shipment.model.toResponse
import com.buyeong.umji.api.shipment.service.ShipmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "운영 배송 관리", description = "주문 송장 등록과 배송 완료 상태 관리 API")
class OperationShipmentController(
    private val currentAccounts: CurrentAccountService,
    private val shipments: ShipmentService,
) {
    @Operation(summary = "주문 송장 정보 등록", description = "주문 송장 정보 등록 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PutMapping("/tracking")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_WRITE')")
    fun registerTracking(
        @Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID,
        @Valid @RequestBody request: ShipmentTrackingRequest,
    ): ShipmentResponse = shipments.registerTracking(
        orderId,
        request.carrierCode,
        request.trackingNumber,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()

    @Operation(summary = "주문 배송 완료 처리", description = "주문 배송 완료 처리 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/delivered")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_WRITE')")
    fun markDelivered(@Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID): ShipmentResponse = shipments.markDelivered(
        orderId,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()
}