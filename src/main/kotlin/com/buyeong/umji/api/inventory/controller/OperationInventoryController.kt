package com.buyeong.umji.api.inventory.controller

import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.inventory.service.InventoryService
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
    private val inventoryService: InventoryService,
) {
    @GetMapping("/skus/{skuId}")
    fun stock(@PathVariable skuId: UUID): InventoryStockResponse = inventoryService.stock(skuId)

    @PatchMapping("/skus/{skuId}")
    fun adjust(
        @PathVariable skuId: UUID,
        @Valid @RequestBody request: AdjustInventoryRequest,
    ): InventoryStockResponse = inventoryService.adjust(skuId, request)

    @GetMapping("/skus/{skuId}/movements")
    fun movements(
        @PathVariable skuId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): InventoryMovementPageResponse = inventoryService.movements(skuId, page, size)
}
