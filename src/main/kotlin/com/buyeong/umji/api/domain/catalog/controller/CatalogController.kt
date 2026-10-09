package com.buyeong.umji.api.domain.catalog.controller

import com.buyeong.umji.api.domain.catalog.dto.ProductDetailViewDto
import com.buyeong.umji.api.domain.catalog.dto.ProductPageViewDto
import com.buyeong.umji.api.domain.catalog.model.CategoryResponse
import com.buyeong.umji.api.domain.catalog.model.ProductDetailResponse
import com.buyeong.umji.api.domain.catalog.model.ProductPageResponse
import com.buyeong.umji.api.domain.catalog.model.ProductSkuResponse
import com.buyeong.umji.api.domain.catalog.model.ProductSummaryResponse
import com.buyeong.umji.api.domain.catalog.service.CatalogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "상품 조회", description = "앱 카테고리·상품·SKU 조회 API")
class CatalogController(
    private val catalogService: CatalogService,
) {
    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/categories")
    fun categories(@RequestParam(defaultValue = "WHOLESALE") channel: String): List<CategoryResponse> = catalogService.categories(channel).map {
        CategoryResponse(it.id, it.name, it.path, it.depth, it.channelCode)
    }

    @Operation(summary = "상품조회", description = "앱 카테고리·상품·SKU 조회 API. /products 경로에서 상품조회를 수행")
    @GetMapping("/products")
    fun products(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @Parameter(description = "판매 채널 코드. 누락 시 WHOLESALE") @RequestParam(defaultValue = "WHOLESALE") channel: String,
    ): ProductPageResponse = catalogService.products(page, size, channel).toResponse()

    @Operation(summary = "상품조회", description = "앱 카테고리·상품·SKU 조회 API. /products/{productId} 경로에서 상품조회를 수행")
    @GetMapping("/products/{productId}")
    fun product(
        @Parameter(description = "상품 공개 식별자(UUID)") @PathVariable productId: UUID,
        @Parameter(description = "판매 채널 코드. 누락 시 WHOLESALE") @RequestParam(defaultValue = "WHOLESALE") channel: String,
    ): ProductDetailResponse = catalogService.product(productId, channel).toResponse()

    private fun ProductPageViewDto.toResponse() = ProductPageResponse(
        items.map { ProductSummaryResponse(it.id, it.name, it.brandName, it.channelCode, it.startingPrice, it.startingUnitsPerSale) },
        page,
        size,
        totalElements,
        totalPages,
    )

    private fun ProductDetailViewDto.toResponse() = ProductDetailResponse(
        id,
        name,
        description,
        categoryName,
        brandName,
        skus.map { ProductSkuResponse(it.id, it.code, it.name, it.salePrice, it.listPrice, it.salesOfferId, it.unitsPerSale, it.sellerOrganizationId) },
        channelCode,
    )
}