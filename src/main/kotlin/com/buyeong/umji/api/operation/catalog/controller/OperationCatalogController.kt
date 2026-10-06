package com.buyeong.umji.api.operation.catalog.controller

import com.buyeong.umji.api.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.operation.catalog.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.model.ChannelCategoryCommand
import com.buyeong.umji.api.operation.catalog.model.ChannelListingCommand
import com.buyeong.umji.api.operation.catalog.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.model.OptionValueCommand
import com.buyeong.umji.api.operation.catalog.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.model.ProductView
import com.buyeong.umji.api.operation.catalog.model.SalesOfferCommand
import com.buyeong.umji.api.operation.catalog.model.SalesOfferView
import com.buyeong.umji.api.operation.catalog.model.SkuCommand
import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateCategoryRequest
import com.buyeong.umji.api.operation.model.CreateChannelCategoryRequest
import com.buyeong.umji.api.operation.model.CreateProductImageRequest
import com.buyeong.umji.api.operation.model.CreateProductOptionRequest
import com.buyeong.umji.api.operation.model.CreateProductRequest
import com.buyeong.umji.api.operation.model.CreateProductSkuRequest
import com.buyeong.umji.api.operation.model.OperationBrandResponse
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import com.buyeong.umji.api.operation.model.OperationCategoryResponse
import com.buyeong.umji.api.operation.model.OperationProductImageResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionValueResponse
import com.buyeong.umji.api.operation.model.OperationProductPageResponse
import com.buyeong.umji.api.operation.model.OperationProductResponse
import com.buyeong.umji.api.operation.model.OperationProductSkuResponse
import com.buyeong.umji.api.operation.model.OperationSalesOfferResponse
import com.buyeong.umji.api.operation.model.UpdateChannelListingRequest
import com.buyeong.umji.api.operation.model.UpdateProductRequest
import com.buyeong.umji.api.operation.model.UpdateProductStatusRequest
import com.buyeong.umji.api.operation.model.UpdateSalesOfferRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation")
@Validated
@Tag(name = "운영 상품 관리", description = "카테고리·브랜드·상품·옵션·SKU 관리 API")
class OperationCatalogController(private val useCase: OperationCatalogService) {
    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun categories() = useCase.categories("WHOLESALE").map { OperationCategoryResponse(it.id, it.parentId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus) }

    @Operation(summary = "채널 카테고리 목록 조회", description = "판매 채널에 연결된 카테고리 트리를 조회")
    @GetMapping("/channels/{channelCode}/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun channelCategories(@PathVariable channelCode: String) =
        useCase.categories(channelCode).map { OperationCategoryResponse(it.id, it.parentId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus) }

    @Operation(summary = "채널 카테고리 등록", description = "지정한 판매 채널에 전용 카테고리를 등록")
    @PostMapping("/channels/{channelCode}/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createChannelCategory(
        @PathVariable channelCode: String,
        @Valid @RequestBody request: CreateChannelCategoryRequest,
    ) = useCase.createChannelCategory(
        ChannelCategoryCommand(channelCode, request.name, request.parentId, request.displayOrder, request.displayStatus),
    ).toResponse()

    @Operation(summary = "채널별 상품 전시 수정", description = "공용 상품을 채널 카테고리에 연결하고 채널별 노출·순서를 수정")
    @PutMapping("/channels/{channelCode}/products/{productId}/listing")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateChannelListing(
        @PathVariable channelCode: String,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateChannelListingRequest,
    ) = useCase.updateChannelListing(
        ChannelListingCommand(channelCode, productId, request.categoryId, request.displayStatus, request.displayOrder),
    ).toResponse()

    @Operation(summary = "채널별 SKU 판매 오퍼 수정", description = "공용 실물 SKU의 채널별 판매가·정가·판매 상태를 수정")
    @PutMapping("/channels/{channelCode}/skus/{skuId}/offer")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateSalesOffer(
        @PathVariable channelCode: String,
        @PathVariable skuId: UUID,
        @Valid @RequestBody request: UpdateSalesOfferRequest,
    ) = useCase.updateSalesOffer(
        SalesOfferCommand(channelCode, skuId, request.salePrice, request.listPrice, request.salesStatus, request.unitsPerSale),
    ).toResponse()

    @Operation(summary = "브랜드 목록 조회", description = "브랜드 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/brands")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun brands(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) = useCase.brands(page, size).map { OperationBrandResponse(it.id, it.name, it.displayStatus) }

    @Operation(summary = "상품조회", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products 경로에서 상품조회를 수행")
    @GetMapping("/products")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun products(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationProductPageResponse {
        val p = useCase.products(page, size)
        return OperationProductPageResponse(p.items.map { it.toResponse() }, p.page, p.size, p.totalElements, p.totalPages)
    }

    @Operation(summary = "상품조회", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId} 경로에서 상품조회를 수행")
    @GetMapping("/products/{productId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun product(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
    ) = useCase.product(productId).toResponse()

    @Operation(summary = "카테고리 등록", description = "카테고리 등록 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @Valid @RequestBody r: CreateCategoryRequest,
    ) = useCase.createCategory(CategoryCommand(r.name, r.parentId, r.displayOrder, r.displayStatus)).toResponse()

    @Operation(summary = "브랜드 등록", description = "브랜드 등록 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/brands")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBrand(
        @Valid @RequestBody r: CreateBrandRequest,
    ) = useCase.createBrand(BrandCommand(r.name, r.displayStatus)).toResponse()

    @Operation(summary = "상품등록", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products 경로에서 상품등록를 수행")
    @PostMapping("/products")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(
        @Valid @RequestBody r: CreateProductRequest,
    ) = useCase.createProduct(r.toCommand()).toResponse()

    @Operation(summary = "상품수정", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId} 경로에서 상품수정를 수행")
    @PatchMapping("/products/{productId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateProduct(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Valid @RequestBody r: UpdateProductRequest,
    ) = useCase.updateProduct(productId, r.toCommand()).toResponse()

    @Operation(summary = "상품등록", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId}/images 경로에서 상품등록를 수행")
    @PostMapping("/products/{productId}/images")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addImage(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductImageRequest,
    ) = useCase.addImage(productId, r.toCommand()).toResponse()

    @Operation(summary = "상품등록", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId}/options 경로에서 상품등록를 수행")
    @PostMapping("/products/{productId}/options")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addOption(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductOptionRequest,
    ) = useCase.addOption(productId, r.toCommand()).toResponse()

    @Operation(summary = "상품등록", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId}/skus 경로에서 상품등록를 수행")
    @PostMapping("/products/{productId}/skus")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addSku(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductSkuRequest,
    ) = useCase.addSku(productId, r.toCommand()).toResponse()

    @Operation(summary = "상품수정", description = "카테고리·브랜드·상품·옵션·SKU 관리 API. /products/{productId}/status 경로에서 상품수정를 수행")
    @PatchMapping("/products/{productId}/status")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateStatus(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Valid @RequestBody r: UpdateProductStatusRequest,
    ) = useCase.updateStatus(productId, ProductStatusCommand(r.displayStatus, r.salesStatus)).toResponse()

    private fun CreateProductRequest.toCommand() = ProductCommand(
        categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder,
        images.map {
            it.toCommand()
        },
        options.map { it.toCommand() }, skus.map { it.toCommand() },
    )
    private fun UpdateProductRequest.toCommand() = ProductCommand(categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder)
    private fun CreateProductImageRequest.toCommand() = ImageCommand(storageKey, altText, displayOrder)
    private fun CreateProductOptionRequest.toCommand() = OptionCommand(name, displayOrder, values.map { OptionValueCommand(it.value, it.displayOrder) })
    private fun CreateProductSkuRequest.toCommand() = SkuCommand(skuCode, name, salePrice, listPrice, salesStatus, optionValueIds)
    private fun CatalogResource.toResponse() = OperationCatalogResourceResponse(id)
    private fun SalesOfferView.toResponse() = OperationSalesOfferResponse(id, channelCode, skuId, salePrice, listPrice, salesStatus, unitsPerSale)
    private fun ProductView.toResponse() = OperationProductResponse(
        id, categoryId, brandId, name, description, displayStatus, salesStatus, displayOrder,
        images.map {
            OperationProductImageResponse(it.id, it.storageKey, it.altText, it.displayOrder)
        },
        options.map {
            OperationProductOptionResponse(it.id, it.name, it.displayOrder, it.values.map { v -> OperationProductOptionValueResponse(v.id, v.value, v.displayOrder) })
        },
        skus.map { OperationProductSkuResponse(it.id, it.skuCode, it.name, it.salePrice, it.listPrice, it.salesStatus, it.optionValueIds) },
    )
}
