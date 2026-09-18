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
)

data class CreateProductRequest(
    @field:NotNull val categoryId: UUID,
    val brandId: UUID? = null,
    @field:NotBlank @field:Size(max = 200) val name: String,
    val description: String? = null,
    @field:NotBlank val displayStatus: String = "HIDDEN",
    @field:NotBlank val salesStatus: String = "ON_SALE",
    @field:Min(0) val displayOrder: Int = 0,
    @field:NotEmpty val skus: List<@Valid CreateProductSkuRequest>,
)

data class UpdateProductStatusRequest(
    @field:NotBlank val displayStatus: String,
    @field:NotBlank val salesStatus: String,
)
