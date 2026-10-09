package com.buyeong.umji.api.seller.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.inventory.dto.MovementPageStateDto
import com.buyeong.umji.api.inventory.dto.MovementStateDto
import com.buyeong.umji.api.inventory.dto.StockViewDto
import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryMovementResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.operation.catalog.dto.CatalogResourceDto
import com.buyeong.umji.api.operation.catalog.dto.ImageCommandDto
import com.buyeong.umji.api.operation.catalog.dto.OptionCommandDto
import com.buyeong.umji.api.operation.catalog.dto.OptionValueCommandDto
import com.buyeong.umji.api.operation.catalog.dto.ProductCommandDto
import com.buyeong.umji.api.operation.catalog.dto.ProductPageViewDto
import com.buyeong.umji.api.operation.catalog.dto.ProductViewDto
import com.buyeong.umji.api.operation.catalog.dto.SkuCommandDto
import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateProductRequest
import com.buyeong.umji.api.operation.model.OperationBrandResponse
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import com.buyeong.umji.api.operation.model.OperationProductImageResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionValueResponse
import com.buyeong.umji.api.operation.model.OperationProductPageResponse
import com.buyeong.umji.api.operation.model.OperationProductResponse
import com.buyeong.umji.api.operation.model.OperationProductSkuResponse
import com.buyeong.umji.api.operation.model.UpdateChannelListingRequest
import com.buyeong.umji.api.operation.model.UpdateProductRequest
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/seller")
@Tag(name = "판매자 상품", description = "판매 Organization의 상품 오퍼 관리 API")
class SellerCatalogController(
    private val currentAccounts: CurrentAccountService,
    private val sellers: SellerCatalogService,
) {
    @Operation(summary = "판매자 브랜드 목록", description = "활성 판매 Organization 소유 브랜드 조회")
    @GetMapping("/brands")
    fun brands(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): List<OperationBrandResponse> = sellers.brands(currentAccounts.activeAccountPublicId(), page, size)
        .map { OperationBrandResponse(it.id, it.name, it.displayStatus) }

    @Operation(summary = "판매자 브랜드 등록")
    @org.springframework.web.bind.annotation.PostMapping("/brands")
    fun createBrand(@Valid @RequestBody request: CreateBrandRequest): OperationCatalogResourceResponse =
        sellers.createBrand(currentAccounts.activeAccountPublicId(), request.name, request.displayStatus).toResponse()

    @Operation(summary = "판매자 상품 목록", description = "활성 판매 Organization 소유 상품 조회")
    @GetMapping("/products")
    fun products(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationProductPageResponse = sellers.products(currentAccounts.activeAccountPublicId(), page, size).toResponse()

    @Operation(summary = "판매자 상품 상세", description = "활성 판매 Organization 소유 상품 및 SKU·옵션·이미지 조회")
    @GetMapping("/products/{productId}")
    fun product(@PathVariable productId: UUID): OperationProductResponse =
        sellers.product(currentAccounts.activeAccountPublicId(), productId).toResponse()

    @Operation(summary = "판매자 상품 등록", description = "판매 Organization·브랜드에 귀속된 상품과 SKU 등록")
    @org.springframework.web.bind.annotation.PostMapping("/products")
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): OperationCatalogResourceResponse =
        sellers.createProduct(currentAccounts.activeAccountPublicId(), request.toCommand()).toResponse()

    @Operation(summary = "판매자 상품 수정", description = "본인 Organization 소유 상품 정보 수정")
    @PatchMapping("/products/{productId}")
    fun updateProduct(@PathVariable productId: UUID, @Valid @RequestBody request: UpdateProductRequest): OperationCatalogResourceResponse =
        sellers.updateProduct(currentAccounts.activeAccountPublicId(), productId, request.toCommand()).toResponse()

    @Operation(summary = "판매자 채널 상품 노출 수정", description = "본인 Organization 상품의 채널별 카테고리·노출·순서 관리")
    @org.springframework.web.bind.annotation.PutMapping("/channels/{channelCode}/products/{productId}/listing")
    fun updateListing(
        @PathVariable channelCode: String,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateChannelListingRequest,
    ): OperationCatalogResourceResponse = sellers.updateListing(
        currentAccounts.activeAccountPublicId(),
        channelCode,
        productId,
        request.categoryId,
        request.displayStatus,
        request.displayOrder,
    ).toResponse()

    @Operation(summary = "판매 오퍼 등록·수정", description = "활성 SELLER Organization의 채널별 판매가·상태·판매 단위를 등록하거나 수정")
    @PutMapping("/channels/{channelCode}/skus/{skuId}/offer")
    fun updateOffer(
        @PathVariable channelCode: String,
        @PathVariable skuId: UUID,
        @Valid @RequestBody request: UpdateSalesOfferRequest,
    ): SellerSalesOfferResponse {
        val view = sellers.updateOffer(
            currentAccounts.activeAccountPublicId(),
            channelCode,
            skuId,
            request.salePrice,
            request.listPrice,
            request.salesStatus,
            request.unitsPerSale,
        )
        return SellerSalesOfferResponse(
            view.id,
            requireNotNull(view.organizationId),
            view.channelCode,
            view.skuId,
            view.salePrice,
            view.listPrice,
            view.salesStatus,
            view.unitsPerSale,
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
            result.number,
            result.size,
            result.totalElements,
            result.totalPages,
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
            currentAccounts.activeAccountPublicId(),
            skuId,
            request.quantityDelta,
            request.reason,
            request.memo,
            request.safetyStockQuantity,
        ).toResponse()

    @Operation(summary = "판매자 재고 변동 조회", description = "활성 SELLER Organization의 SKU별 재고 변동 이력을 조회")
    @GetMapping("/inventory/skus/{skuId}/movements")
    fun movements(
        @PathVariable skuId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): InventoryMovementPageResponse = sellers.movements(currentAccounts.activeAccountPublicId(), skuId, page, size).toResponse()

    private fun StockViewDto.toResponse() = InventoryStockResponse(
        skuId, skuCode, onHand, reserved, available, safety, unitsPerSale, onHandBoxes, onHandRemainder,
        reservedBoxes, reservedRemainder, availableBoxes, availableRemainder,
    )
    private fun MovementPageStateDto.toResponse() = InventoryMovementPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)
    private fun MovementStateDto.toResponse() = InventoryMovementResponse(id, sku.id, sku.code, type, delta, referenceType, referenceId, memo, occurredAt)

    private fun CreateProductRequest.toCommand() = ProductCommandDto(
        categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder,
        images.map { ImageCommandDto(it.storageKey, it.altText, it.displayOrder) },
        options.map { OptionCommandDto(it.name, it.displayOrder, it.values.map { v -> OptionValueCommandDto(v.value, v.displayOrder) }) },
        skus.map { SkuCommandDto(it.skuCode, it.name, it.salePrice, it.listPrice, it.salesStatus, it.optionValueIds) },
    )

    private fun UpdateProductRequest.toCommand() = ProductCommandDto(categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder)
    private fun CatalogResourceDto.toResponse() = OperationCatalogResourceResponse(id)
    private fun com.buyeong.umji.api.operation.catalog.dto.ProductPageViewDto.toResponse() =
        OperationProductPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)
    private fun ProductViewDto.toResponse() = OperationProductResponse(
        id, categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder,
        images.map { OperationProductImageResponse(it.id, it.storageKey, it.altText, it.displayOrder) },
        options.map { OperationProductOptionResponse(it.id, it.name, it.displayOrder, it.values.map { v -> OperationProductOptionValueResponse(v.id, v.value, v.displayOrder) }) },
        skus.map { OperationProductSkuResponse(it.id, it.skuCode, it.name, it.salePrice, it.listPrice, it.salesStatus, it.optionValueIds) },
    )
}