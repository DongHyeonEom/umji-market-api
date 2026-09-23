package com.buyeong.umji.api.operation.catalog.adapter.`in`.web

import com.buyeong.umji.api.operation.catalog.application.model.*
import com.buyeong.umji.api.operation.catalog.application.port.`in`.OperationCatalogUseCase
import com.buyeong.umji.api.operation.model.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController @RequestMapping("/api/operation") @Validated
class OperationCatalogController(private val useCase:OperationCatalogUseCase) {
    @GetMapping("/categories") fun categories()=useCase.categories().map { OperationCategoryResponse(it.id,it.parentId,it.name,it.path,it.depth,it.displayOrder,it.displayStatus) }
    @GetMapping("/brands") fun brands(@RequestParam(defaultValue="0") @Min(0) page:Int,@RequestParam(defaultValue="20") @Min(1) @Max(100) size:Int)=useCase.brands(page,size).map { OperationBrandResponse(it.id,it.name,it.displayStatus) }
    @GetMapping("/products") fun products(@RequestParam(defaultValue="0") @Min(0) page:Int,@RequestParam(defaultValue="20") @Min(1) @Max(100) size:Int):OperationProductPageResponse { val p=useCase.products(page,size); return OperationProductPageResponse(p.items.map { it.toResponse() },p.page,p.size,p.totalElements,p.totalPages) }
    @GetMapping("/products/{productId}") fun product(@PathVariable productId:UUID)=useCase.product(productId).toResponse()
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) fun createCategory(@Valid @RequestBody r:CreateCategoryRequest)=useCase.createCategory(CategoryCommand(r.name,r.parentId,r.displayOrder,r.displayStatus)).toResponse()
    @PostMapping("/brands") @ResponseStatus(HttpStatus.CREATED) fun createBrand(@Valid @RequestBody r:CreateBrandRequest)=useCase.createBrand(BrandCommand(r.name,r.displayStatus)).toResponse()
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED) fun createProduct(@Valid @RequestBody r:CreateProductRequest)=useCase.createProduct(r.toCommand()).toResponse()
    @PatchMapping("/products/{productId}") fun updateProduct(@PathVariable productId:UUID,@Valid @RequestBody r:UpdateProductRequest)=useCase.updateProduct(productId,r.toCommand()).toResponse()
    @PostMapping("/products/{productId}/images") @ResponseStatus(HttpStatus.CREATED) fun addImage(@PathVariable productId:UUID,@Valid @RequestBody r:CreateProductImageRequest)=useCase.addImage(productId,r.toCommand()).toResponse()
    @PostMapping("/products/{productId}/options") @ResponseStatus(HttpStatus.CREATED) fun addOption(@PathVariable productId:UUID,@Valid @RequestBody r:CreateProductOptionRequest)=useCase.addOption(productId,r.toCommand()).toResponse()
    @PostMapping("/products/{productId}/skus") @ResponseStatus(HttpStatus.CREATED) fun addSku(@PathVariable productId:UUID,@Valid @RequestBody r:CreateProductSkuRequest)=useCase.addSku(productId,r.toCommand()).toResponse()
    @PatchMapping("/products/{productId}/status") fun updateStatus(@PathVariable productId:UUID,@Valid @RequestBody r:UpdateProductStatusRequest)=useCase.updateStatus(productId,ProductStatusCommand(r.displayStatus,r.salesStatus)).toResponse()

    private fun CreateProductRequest.toCommand()=ProductCommand(categoryId,brandId,name,description,displayStatus,salesStatus,displayOrder,images.map { it.toCommand() },options.map { it.toCommand() },skus.map { it.toCommand() })
    private fun UpdateProductRequest.toCommand()=ProductCommand(categoryId,brandId,name,description,displayStatus,salesStatus,displayOrder)
    private fun CreateProductImageRequest.toCommand()=ImageCommand(storageKey,altText,displayOrder)
    private fun CreateProductOptionRequest.toCommand()=OptionCommand(name,displayOrder,values.map { OptionValueCommand(it.value,it.displayOrder) })
    private fun CreateProductSkuRequest.toCommand()=SkuCommand(skuCode,name,salePrice,listPrice,salesStatus,optionValueIds)
    private fun CatalogResource.toResponse()=OperationCatalogResourceResponse(id)
    private fun ProductView.toResponse()=OperationProductResponse(id,categoryId,brandId,name,description,displayStatus,salesStatus,displayOrder,images.map { OperationProductImageResponse(it.id,it.storageKey,it.altText,it.displayOrder) },options.map { OperationProductOptionResponse(it.id,it.name,it.displayOrder,it.values.map { v->OperationProductOptionValueResponse(v.id,v.value,v.displayOrder) }) },skus.map { OperationProductSkuResponse(it.id,it.skuCode,it.name,it.salePrice,it.listPrice,it.salesStatus,it.optionValueIds) })
}
