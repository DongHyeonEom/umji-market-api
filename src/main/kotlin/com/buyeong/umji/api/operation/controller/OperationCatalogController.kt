package com.buyeong.umji.api.operation.controller

import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateCategoryRequest
import com.buyeong.umji.api.operation.model.CreateProductRequest
import com.buyeong.umji.api.operation.model.CreateProductImageRequest
import com.buyeong.umji.api.operation.model.CreateProductOptionRequest
import com.buyeong.umji.api.operation.model.CreateProductSkuRequest
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import com.buyeong.umji.api.operation.model.OperationBrandResponse
import com.buyeong.umji.api.operation.model.OperationCategoryResponse
import com.buyeong.umji.api.operation.model.OperationProductPageResponse
import com.buyeong.umji.api.operation.model.OperationProductResponse
import com.buyeong.umji.api.operation.model.UpdateProductStatusRequest
import com.buyeong.umji.api.operation.model.UpdateProductRequest
import com.buyeong.umji.api.operation.service.OperationCatalogService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation")
@Validated
class OperationCatalogController(
    private val operationCatalogService: OperationCatalogService,
) {
    @GetMapping("/categories")
    fun categories(): List<OperationCategoryResponse> = operationCatalogService.categories()

    @GetMapping("/brands")
    fun brands(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): List<OperationBrandResponse> = operationCatalogService.brands(page, size)

    @GetMapping("/products")
    fun products(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationProductPageResponse = operationCatalogService.products(page, size)

    @GetMapping("/products/{productId}")
    fun productDetail(@PathVariable productId: UUID): OperationProductResponse = operationCatalogService.productDetail(productId)

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(@Valid @RequestBody request: CreateCategoryRequest): OperationCatalogResourceResponse =
        operationCatalogService.createCategory(request)

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBrand(@Valid @RequestBody request: CreateBrandRequest): OperationCatalogResourceResponse =
        operationCatalogService.createBrand(request)

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): OperationCatalogResourceResponse =
        operationCatalogService.createProduct(request)

    @PatchMapping("/products/{productId}")
    fun updateProduct(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateProductRequest,
    ): OperationCatalogResourceResponse = operationCatalogService.updateProduct(productId, request)

    @PostMapping("/products/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProductImage(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: CreateProductImageRequest,
    ): OperationCatalogResourceResponse = operationCatalogService.addProductImage(productId, request)

    @PostMapping("/products/{productId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProductOption(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: CreateProductOptionRequest,
    ): OperationCatalogResourceResponse = operationCatalogService.addProductOption(productId, request)

    @PostMapping("/products/{productId}/skus")
    @ResponseStatus(HttpStatus.CREATED)
    fun addProductSku(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: CreateProductSkuRequest,
    ): OperationCatalogResourceResponse = operationCatalogService.addProductSku(productId, request)

    @PatchMapping("/products/{productId}/status")
    fun updateProductStatus(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateProductStatusRequest,
    ): OperationCatalogResourceResponse = operationCatalogService.updateProductStatus(productId, request)
}
