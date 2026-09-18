package com.buyeong.umji.api.catalog.controller

import com.buyeong.umji.api.catalog.model.CategoryResponse
import com.buyeong.umji.api.catalog.model.ProductDetailResponse
import com.buyeong.umji.api.catalog.model.ProductPageResponse
import com.buyeong.umji.api.catalog.service.CatalogService
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
    private val catalogService: CatalogService,
) {
    @GetMapping("/categories")
    fun categories(): List<CategoryResponse> = catalogService.categories()

    @GetMapping("/products")
    fun products(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ProductPageResponse = catalogService.products(page, size)

    @GetMapping("/products/{productId}")
    fun product(@PathVariable productId: UUID): ProductDetailResponse = catalogService.product(productId)
}
