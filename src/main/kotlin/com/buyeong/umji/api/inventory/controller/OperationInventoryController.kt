package com.buyeong.umji.api.inventory.controller

import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryMovementResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.inventory.model.MovementPageState
import com.buyeong.umji.api.inventory.model.MovementState
import com.buyeong.umji.api.inventory.model.StockView
import com.buyeong.umji.api.inventory.service.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/inventory")
@Validated
@Tag(name = "운영 재고", description = "SKU 재고 조회·조정과 변동 이력 관리 API")
class OperationInventoryController(
    private val inventoryService: InventoryService,
) {
    @Operation(summary = "상품 옵션조회", description = "SKU 재고 조회·조정과 변동 이력 관리 API. /skus/{skuId} 경로에서 상품 옵션조회를 수행")
    @GetMapping("/skus/{skuId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'INVENTORY_READ')")
    fun stock(@Parameter(description = "상품 옵션 공개 식별자(UUID)") @PathVariable skuId: UUID): InventoryStockResponse = inventoryService.stock(skuId).toResponse()

    @Operation(summary = "상품 옵션수정", description = "SKU 재고 조회·조정과 변동 이력 관리 API. /skus/{skuId} 경로에서 상품 옵션수정를 수행")
    @PatchMapping("/skus/{skuId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'INVENTORY_WRITE')")
    fun adjust(
        @Parameter(description = "상품 옵션 공개 식별자(UUID)") @PathVariable skuId: UUID,
        @Valid @RequestBody request: AdjustInventoryRequest,
    ): InventoryStockResponse = inventoryService.adjust(skuId, request.quantityDelta, request.reason, request.memo, request.safetyStockQuantity).toResponse()

    @Operation(summary = "상품 옵션조회", description = "SKU 재고 조회·조정과 변동 이력 관리 API. /skus/{skuId}/movements 경로에서 상품 옵션조회를 수행")
    @GetMapping("/skus/{skuId}/movements")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'INVENTORY_READ')")
    fun movements(
        @Parameter(description = "상품 옵션 공개 식별자(UUID)") @PathVariable skuId: UUID,
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): InventoryMovementPageResponse = inventoryService.movements(skuId, page, size).toResponse()

    private fun StockView.toResponse() = InventoryStockResponse(
        skuId, skuCode, onHand, reserved, available, safety, unitsPerSale, onHandBoxes, onHandRemainder,
        reservedBoxes, reservedRemainder, availableBoxes, availableRemainder,
    )
    private fun MovementPageState.toResponse() = InventoryMovementPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)
    private fun MovementState.toResponse() = InventoryMovementResponse(id, sku.id, sku.code, type, delta, referenceType, referenceId, memo, occurredAt)
}
