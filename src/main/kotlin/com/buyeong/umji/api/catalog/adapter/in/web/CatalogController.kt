package com.buyeong.umji.api.catalog.adapter.`in`.web

import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import com.buyeong.umji.api.catalog.application.port.`in`.CatalogUseCase
import com.buyeong.umji.api.catalog.model.CategoryResponse
import com.buyeong.umji.api.catalog.model.ProductDetailResponse
import com.buyeong.umji.api.catalog.model.ProductPageResponse
import com.buyeong.umji.api.catalog.model.ProductSkuResponse
import com.buyeong.umji.api.catalog.model.ProductSummaryResponse
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
class CatalogController(
    private val catalogService: CatalogUseCase,
) {
    @GetMapping("/categories")
    fun categories(): List<CategoryResponse> = catalogService.categories().map {
        CategoryResponse(it.id, it.name, it.path, it.depth)
    }

    @GetMapping("/products")
    fun products(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ProductPageResponse = catalogService.products(page, size).toResponse()

    @GetMapping("/products/{productId}")
    fun product(@PathVariable productId: UUID): ProductDetailResponse = catalogService.product(productId).toResponse()

    private fun ProductPageView.toResponse() = ProductPageResponse(
        items.map { ProductSummaryResponse(it.id, it.name, it.brandName) },
        page,
        size,
        totalElements,
        totalPages,
    )

    private fun ProductDetailView.toResponse() = ProductDetailResponse(
        id,
        name,
        description,
        categoryName,
        brandName,
        skus.map { ProductSkuResponse(it.id, it.code, it.name, it.salePrice, it.listPrice) },
    )
}