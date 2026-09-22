package com.buyeong.umji.api.operation.service

import com.buyeong.umji.api.persistence.jpa.catalog.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductImageEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductOptionEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductOptionValueEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateCategoryRequest
import com.buyeong.umji.api.operation.model.CreateProductRequest
import com.buyeong.umji.api.operation.model.CreateProductSkuRequest
import com.buyeong.umji.api.operation.model.CreateProductImageRequest
import com.buyeong.umji.api.operation.model.CreateProductOptionRequest
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import com.buyeong.umji.api.operation.model.OperationBrandResponse
import com.buyeong.umji.api.operation.model.OperationCategoryResponse
import com.buyeong.umji.api.operation.model.OperationProductPageResponse
import com.buyeong.umji.api.operation.model.OperationProductResponse
import com.buyeong.umji.api.operation.model.OperationProductSkuResponse
import com.buyeong.umji.api.operation.model.OperationProductImageResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionResponse
import com.buyeong.umji.api.operation.model.OperationProductOptionValueResponse
import com.buyeong.umji.api.operation.model.UpdateProductRequest
import com.buyeong.umji.api.operation.model.UpdateProductStatusRequest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class OperationCatalogService(
    private val catalog: CatalogJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun categories(): List<OperationCategoryResponse> =
        catalog.categories().map { category ->
            OperationCategoryResponse(
                id = requireNotNull(category.publicId),
                parentId = category.parent?.publicId,
                name = category.name,
                path = category.path,
                depth = category.depth,
                displayOrder = category.displayOrder,
                displayStatus = category.displayStatus,
            )
        }

    @Transactional(readOnly = true)
    fun brands(page: Int, size: Int): List<OperationBrandResponse> =
        catalog.brands(PageRequest.of(page, size, Sort.by("name"))).content.map { brand ->
            OperationBrandResponse(requireNotNull(brand.publicId), brand.name, brand.displayStatus)
        }

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int): OperationProductPageResponse {
        val products = catalog.products(PageRequest.of(page, size, Sort.by("id").descending()))
        return OperationProductPageResponse(
            items = products.content.map(::productResponse),
            page = products.number,
            size = products.size,
            totalElements = products.totalElements,
            totalPages = products.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun productDetail(productId: UUID): OperationProductResponse = productResponse(product(productId), includeSkus = true)

    fun createCategory(request: CreateCategoryRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        val parent = request.parentId?.let { category(it) }
        val category =
            CategoryEntity().apply {
                name = request.name.trim()
                this.parent = parent
                depth = (parent?.depth ?: -1) + 1
                path = parent?.path?.let { "$it/$name" } ?: name
                displayOrder = request.displayOrder
                displayStatus = request.displayStatus
            }
        return OperationCatalogResourceResponse(requireNotNull(catalog.save(category).publicId))
    }

    fun createBrand(request: CreateBrandRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        val name = request.name.trim()
        require(!catalog.existsBrandName(name)) { "이미 존재하는 브랜드입니다." }
        val brand = BrandEntity().apply { this.name = name; displayStatus = request.displayStatus }
        return OperationCatalogResourceResponse(requireNotNull(catalog.save(brand).publicId))
    }

    fun createProduct(request: CreateProductRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        validateSalesStatus(request.salesStatus)
        val product =
            ProductEntity().apply {
                category = category(request.categoryId)
                brand = request.brandId?.let { brand(it) }
                name = request.name.trim()
                description = request.description?.trim()?.ifBlank { null }
                displayStatus = request.displayStatus
                salesStatus = request.salesStatus
                displayOrder = request.displayOrder
            }
        val savedProduct = catalog.save(product)
        request.images.forEach { image -> catalog.save(imageEntity(savedProduct, image)) }
        request.options.forEach { option -> saveOption(savedProduct, option) }
        request.skus.forEach { sku -> catalog.save(skuEntity(savedProduct, sku)) }
        return OperationCatalogResourceResponse(requireNotNull(savedProduct.publicId))
    }

    fun updateProduct(productId: UUID, request: UpdateProductRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        validateSalesStatus(request.salesStatus)
        val product = product(productId)
        product.category = category(request.categoryId)
        product.brand = request.brandId?.let(::brand)
        product.name = request.name.trim()
        product.description = request.description?.trim()?.ifBlank { null }
        product.displayStatus = request.displayStatus
        product.salesStatus = request.salesStatus
        product.displayOrder = request.displayOrder
        return OperationCatalogResourceResponse(requireNotNull(product.publicId))
    }

    fun addProductImage(productId: UUID, request: CreateProductImageRequest): OperationCatalogResourceResponse {
        val image = catalog.save(imageEntity(product(productId), request))
        return OperationCatalogResourceResponse(requireNotNull(image.publicId))
    }

    fun addProductOption(productId: UUID, request: CreateProductOptionRequest): OperationCatalogResourceResponse {
        val option = saveOption(product(productId), request)
        return OperationCatalogResourceResponse(requireNotNull(option.publicId))
    }

    fun addProductSku(productId: UUID, request: CreateProductSkuRequest): OperationCatalogResourceResponse {
        val sku = catalog.save(skuEntity(product(productId), request))
        return OperationCatalogResourceResponse(requireNotNull(sku.publicId))
    }

    fun updateProductStatus(productId: UUID, request: UpdateProductStatusRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        validateSalesStatus(request.salesStatus)
        val product = product(productId)
        product.displayStatus = request.displayStatus
        product.salesStatus = request.salesStatus
        return OperationCatalogResourceResponse(requireNotNull(product.publicId))
    }

    private fun skuEntity(product: ProductEntity, request: CreateProductSkuRequest): ProductSkuEntity {
        validateSalesStatus(request.salesStatus)
        require(!catalog.existsSkuCode(request.skuCode)) { "이미 존재하는 SKU 코드입니다." }
        return ProductSkuEntity().apply {
            this.product = product
            skuCode = request.skuCode.trim()
            name = request.name.trim()
            salePrice = request.salePrice
            listPrice = request.listPrice
            salesStatus = request.salesStatus
            optionValues = optionValues(product, request.optionValueIds)
        }
    }

    private fun imageEntity(product: ProductEntity, request: CreateProductImageRequest): ProductImageEntity =
        ProductImageEntity().apply {
            this.product = product
            storageKey = request.storageKey.trim()
            altText = request.altText?.trim()?.ifBlank { null }
            displayOrder = request.displayOrder
        }

    private fun saveOption(product: ProductEntity, request: CreateProductOptionRequest): ProductOptionEntity {
        val option = catalog.save(ProductOptionEntity().apply {
            this.product = product
            name = request.name.trim()
            displayOrder = request.displayOrder
        })
        request.values.forEach { value ->
            catalog.save(ProductOptionValueEntity().apply {
                this.option = option
                this.value = value.value.trim()
                displayOrder = value.displayOrder
            })
        }
        return option
    }

    private fun optionValues(product: ProductEntity, optionValueIds: Set<UUID>): MutableSet<ProductOptionValueEntity> {
        if (optionValueIds.isEmpty()) return linkedSetOf()
        val values = catalog.optionValues(optionValueIds)
        require(values.size == optionValueIds.size && values.all { it.option.product.id == product.id }) {
            "상품에 속하지 않는 옵션값이 포함되어 있습니다."
        }
        return values.toMutableSet()
    }

    private fun category(categoryId: UUID): CategoryEntity =
        catalog.category(categoryId) ?: throw ItemNotFoundException("카테고리를 찾을 수 없습니다.")

    private fun brand(brandId: UUID): BrandEntity =
        catalog.brand(brandId) ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")

    private fun product(productId: UUID): ProductEntity =
        catalog.product(productId) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")

    private fun productResponse(product: ProductEntity, includeSkus: Boolean = false): OperationProductResponse =
        OperationProductResponse(
            id = requireNotNull(product.publicId),
            categoryId = requireNotNull(product.category.publicId),
            brandId = product.brand?.publicId,
            name = product.name,
            description = product.description,
            displayStatus = product.displayStatus,
            salesStatus = product.salesStatus,
            displayOrder = product.displayOrder,
            skus =
                if (includeSkus) {
                    catalog.skus(requireNotNull(product.id)).map { sku ->
                        OperationProductSkuResponse(
                            requireNotNull(sku.publicId), sku.skuCode, sku.name, sku.salePrice, sku.listPrice, sku.salesStatus,
                            sku.optionValues.mapTo(linkedSetOf()) { requireNotNull(it.publicId) },
                        )
                    }
                } else {
                    emptyList()
                },
            images = if (includeSkus) catalog.images(requireNotNull(product.id)).map {
                OperationProductImageResponse(requireNotNull(it.publicId), it.storageKey, it.altText, it.displayOrder)
            } else emptyList(),
            options = if (includeSkus) catalog.options(requireNotNull(product.id)).map { option ->
                OperationProductOptionResponse(
                    requireNotNull(option.publicId), option.name, option.displayOrder,
                    catalog.optionValues(requireNotNull(option.id)).map {
                        OperationProductOptionValueResponse(requireNotNull(it.publicId), it.value, it.displayOrder)
                    },
                )
            } else emptyList(),
        )

    private fun validateDisplayStatus(status: String) {
        require(status in DISPLAY_STATUSES) { "유효하지 않은 상품 노출 상태입니다." }
    }

    private fun validateSalesStatus(status: String) {
        require(status in SALES_STATUSES) { "유효하지 않은 판매 상태입니다." }
    }

    private companion object {
        val DISPLAY_STATUSES = setOf("DISPLAYED", "HIDDEN")
        val SALES_STATUSES = setOf("ON_SALE", "STOPPED")
    }
}
