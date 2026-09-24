package com.buyeong.umji.api.operation.model

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateCategoryRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    val parentId: UUID? = null,
    @field:Min(0) val displayOrder: Int = 0,
    @field:NotBlank val displayStatus: String = "HIDDEN",
)

data class CreateBrandRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:NotBlank val displayStatus: String = "HIDDEN",
)

data class CreateProductSkuRequest(
    @field:NotBlank @field:Size(max = 64) val skuCode: String,
    @field:NotBlank @field:Size(max = 200) val name: String,
    @field:Min(0) val salePrice: Long,
    @field:Min(0) val listPrice: Long? = null,
    @field:NotBlank val salesStatus: String = "ON_SALE",
    val optionValueIds: Set<UUID> = emptySet(),
)

data class CreateProductImageRequest(
    @field:NotBlank @field:Size(max = 500) val storageKey: String,
    @field:Size(max = 200) val altText: String? = null,
    @field:Min(0) val displayOrder: Int = 0,
)

data class CreateProductOptionValueRequest(
    @field:NotBlank @field:Size(max = 100) val value: String,
    @field:Min(0) val displayOrder: Int = 0,
)

data class CreateProductOptionRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Min(0) val displayOrder: Int = 0,
    @field:NotEmpty val values: List<@Valid CreateProductOptionValueRequest>,
)

data class CreateProductRequest(
    @field:NotNull val categoryId: UUID,
    val brandId: UUID? = null,
    @field:NotBlank @field:Size(max = 200) val name: String,
    val description: String? = null,
    @field:NotBlank val displayStatus: String = "HIDDEN",
    @field:NotBlank val salesStatus: String = "ON_SALE",
    @field:Min(0) val displayOrder: Int = 0,
    val images: List<@Valid CreateProductImageRequest> = emptyList(),
    val options: List<@Valid CreateProductOptionRequest> = emptyList(),
    @field:NotEmpty val skus: List<@Valid CreateProductSkuRequest>,
)

data class UpdateProductRequest(
    @field:NotNull val categoryId: UUID,
    val brandId: UUID? = null,
    @field:NotBlank @field:Size(max = 200) val name: String,
    val description: String? = null,
    @field:NotBlank val displayStatus: String,
    @field:NotBlank val salesStatus: String,
    @field:Min(0) val displayOrder: Int = 0,
)

data class UpdateProductStatusRequest(
    @field:NotBlank val displayStatus: String,
    @field:NotBlank val salesStatus: String,
)