package com.buyeong.umji.api.inventory.adapter.`in`.web

import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.MovementState
import com.buyeong.umji.api.inventory.application.model.StockView
import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryMovementResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/inventory")
@Validated
class OperationInventoryController(
    private val inventoryService: InventoryUseCase,
) {
    @GetMapping("/skus/{skuId}")
    fun stock(@PathVariable skuId: UUID): InventoryStockResponse = inventoryService.stock(skuId).toResponse()

    @PatchMapping("/skus/{skuId}")
    fun adjust(
        @PathVariable skuId: UUID,
        @Valid @RequestBody request: AdjustInventoryRequest,
    ): InventoryStockResponse = inventoryService.adjust(skuId, request.quantityDelta, request.reason, request.memo, request.safetyStockQuantity).toResponse()

    @GetMapping("/skus/{skuId}/movements")
    fun movements(
        @PathVariable skuId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): InventoryMovementPageResponse = inventoryService.movements(skuId, page, size).toResponse()

    private fun StockView.toResponse() = InventoryStockResponse(skuId, skuCode, onHand, reserved, available, safety)
    private fun MovementPageState.toResponse() = InventoryMovementPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)
    private fun MovementState.toResponse() = InventoryMovementResponse(id, sku.id, sku.code, type, delta, referenceType, referenceId, memo, occurredAt)
}
