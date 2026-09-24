package com.buyeong.umji.api.operation.catalog.adapter.`in`.web

import com.buyeong.umji.api.operation.catalog.application.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.application.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.application.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.application.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.application.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.application.model.OptionValueCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductView
import com.buyeong.umji.api.operation.catalog.application.model.SkuCommand
import com.buyeong.umji.api.operation.catalog.application.port.`in`.OperationCatalogUseCase
import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateCategoryRequest
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
import com.buyeong.umji.api.operation.model.UpdateProductRequest
import com.buyeong.umji.api.operation.model.UpdateProductStatusRequest
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation")
@Validated
class OperationCatalogController(private val useCase: OperationCatalogUseCase) {
    @GetMapping("/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun categories() = useCase.categories().map { OperationCategoryResponse(it.id, it.parentId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus) }

    @GetMapping("/brands")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun brands(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) = useCase.brands(page, size).map { OperationBrandResponse(it.id, it.name, it.displayStatus) }

    @GetMapping("/products")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun products(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationProductPageResponse {
        val p = useCase.products(page, size)
        return OperationProductPageResponse(p.items.map { it.toResponse() }, p.page, p.size, p.totalElements, p.totalPages)
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_READ')")
    fun product(
        @PathVariable productId: UUID,
    ) = useCase.product(productId).toResponse()

    @PostMapping("/categories")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @Valid @RequestBody r: CreateCategoryRequest,
    ) = useCase.createCategory(CategoryCommand(r.name, r.parentId, r.displayOrder, r.displayStatus)).toResponse()

    @PostMapping("/brands")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBrand(
        @Valid @RequestBody r: CreateBrandRequest,
    ) = useCase.createBrand(BrandCommand(r.name, r.displayStatus)).toResponse()

    @PostMapping("/products")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(
        @Valid @RequestBody r: CreateProductRequest,
    ) = useCase.createProduct(r.toCommand()).toResponse()

    @PatchMapping("/products/{productId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateProduct(
        @PathVariable productId: UUID,
        @Valid @RequestBody r: UpdateProductRequest,
    ) = useCase.updateProduct(productId, r.toCommand()).toResponse()

    @PostMapping("/products/{productId}/images")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addImage(
        @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductImageRequest,
    ) = useCase.addImage(productId, r.toCommand()).toResponse()

    @PostMapping("/products/{productId}/options")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addOption(
        @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductOptionRequest,
    ) = useCase.addOption(productId, r.toCommand()).toResponse()

    @PostMapping("/products/{productId}/skus")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun addSku(
        @PathVariable productId: UUID,
        @Valid @RequestBody r: CreateProductSkuRequest,
    ) = useCase.addSku(productId, r.toCommand()).toResponse()

    @PatchMapping("/products/{productId}/status")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'PRODUCT_WRITE')")
    fun updateStatus(
        @PathVariable productId: UUID,
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