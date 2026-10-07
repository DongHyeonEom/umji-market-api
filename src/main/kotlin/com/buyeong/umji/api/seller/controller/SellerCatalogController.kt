package com.buyeong.umji.api.seller.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryMovementResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.inventory.model.MovementPageState
import com.buyeong.umji.api.inventory.model.MovementState
import com.buyeong.umji.api.inventory.model.StockView
import com.buyeong.umji.api.operation.model.UpdateSalesOfferRequest
import com.buyeong.umji.api.seller.model.SellerSalesOfferResponse
import com.buyeong.umji.api.seller.model.SellerSkuPageResponse
import com.buyeong.umji.api.seller.model.SellerSkuResponse
import com.buyeong.umji.api.seller.service.SellerCatalogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/seller")
@Tag(name = "판매자 상품", description = "판매 Organization의 상품 오퍼 관리 API")
class SellerCatalogController(
    private val currentAccounts: CurrentAccountService,
    private val sellers: SellerCatalogService,
) {
    @Operation(summary = "판매 오퍼 등록·수정", description = "활성 SELLER Organization의 채널별 판매가·상태·판매 단위를 등록하거나 수정")
    @PutMapping("/channels/{channelCode}/skus/{skuId}/offer")
    fun updateOffer(
        @PathVariable channelCode: String,
        @PathVariable skuId: UUID,
        @Valid @RequestBody request: UpdateSalesOfferRequest,
    ): SellerSalesOfferResponse {
        val view = sellers.updateOffer(
            currentAccounts.activeAccountPublicId(), channelCode, skuId,
            request.salePrice, request.listPrice, request.salesStatus, request.unitsPerSale,
        )
        return SellerSalesOfferResponse(
            view.id, requireNotNull(view.organizationId), view.channelCode, view.skuId,
            view.salePrice, view.listPrice, view.salesStatus, view.unitsPerSale,
        )
    }

    @Operation(summary = "판매 가능 공용 SKU 조회", description = "활성 SELLER Organization이 오퍼를 등록할 수 있는 공개 상태 상품의 SKU를 조회")
    @GetMapping("/skus")
    fun skus(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): SellerSkuPageResponse {
        val result = sellers.skus(currentAccounts.activeAccountPublicId(), page, size)
        return SellerSkuPageResponse(
            result.content.map { SellerSkuResponse(it.id, it.skuCode, it.name, it.productId, it.productName, it.brandName) },
            result.number, result.size, result.totalElements, result.totalPages,
        )
    }

    @Operation(summary = "판매자 재고 조회", description = "활성 SELLER Organization의 SKU별 재고와 예약 수량을 조회")
    @GetMapping("/inventory/skus/{skuId}")
    fun stock(@PathVariable skuId: UUID): InventoryStockResponse =
        sellers.stock(currentAccounts.activeAccountPublicId(), skuId).toResponse()

    @Operation(summary = "판매자 재고 조정", description = "활성 SELLER Organization의 SKU별 실재고를 조정")
    @PatchMapping("/inventory/skus/{skuId}")
    fun adjustStock(@PathVariable skuId: UUID, @Valid @RequestBody request: AdjustInventoryRequest): InventoryStockResponse =
        sellers.adjustStock(
            currentAccounts.activeAccountPublicId(), skuId, request.quantityDelta, request.reason, request.memo, request.safetyStockQuantity,
        ).toResponse()

    @Operation(summary = "판매자 재고 변동 조회", description = "활성 SELLER Organization의 SKU별 재고 변동 이력을 조회")
    @GetMapping("/inventory/skus/{skuId}/movements")
    fun movements(
        @PathVariable skuId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): InventoryMovementPageResponse = sellers.movements(currentAccounts.activeAccountPublicId(), skuId, page, size).toResponse()

    private fun StockView.toResponse() = InventoryStockResponse(
        skuId, skuCode, onHand, reserved, available, safety, unitsPerSale, onHandBoxes, onHandRemainder,
        reservedBoxes, reservedRemainder, availableBoxes, availableRemainder,
    )
    private fun MovementPageState.toResponse() = InventoryMovementPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)
    private fun MovementState.toResponse() = InventoryMovementResponse(id, sku.id, sku.code, type, delta, referenceType, referenceId, memo, occurredAt)
}
