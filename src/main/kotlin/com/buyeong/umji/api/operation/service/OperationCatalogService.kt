package com.buyeong.umji.api.operation.service

import com.buyeong.umji.api.catalog.persistence.BrandEntity
import com.buyeong.umji.api.catalog.persistence.BrandRepository
import com.buyeong.umji.api.catalog.persistence.CategoryEntity
import com.buyeong.umji.api.catalog.persistence.CategoryRepository
import com.buyeong.umji.api.catalog.persistence.ProductEntity
import com.buyeong.umji.api.catalog.persistence.ProductRepository
import com.buyeong.umji.api.catalog.persistence.ProductSkuEntity
import com.buyeong.umji.api.catalog.persistence.ProductSkuRepository
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.model.CreateBrandRequest
import com.buyeong.umji.api.operation.model.CreateCategoryRequest
import com.buyeong.umji.api.operation.model.CreateProductRequest
import com.buyeong.umji.api.operation.model.CreateProductSkuRequest
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import com.buyeong.umji.api.operation.model.OperationBrandResponse
import com.buyeong.umji.api.operation.model.OperationCategoryResponse
import com.buyeong.umji.api.operation.model.OperationProductPageResponse
import com.buyeong.umji.api.operation.model.OperationProductResponse
import com.buyeong.umji.api.operation.model.OperationProductSkuResponse
import com.buyeong.umji.api.operation.model.UpdateProductStatusRequest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class OperationCatalogService(
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productSkuRepository: ProductSkuRepository,
) {
    @Transactional(readOnly = true)
    fun categories(): List<OperationCategoryResponse> =
        categoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscNameAsc().map { category ->
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
        brandRepository.findAllByDeletedAtIsNull(PageRequest.of(page, size, Sort.by("name"))).content.map { brand ->
            OperationBrandResponse(requireNotNull(brand.publicId), brand.name, brand.displayStatus)
        }

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int): OperationProductPageResponse {
        val products = productRepository.findAllByDeletedAtIsNull(PageRequest.of(page, size, Sort.by("id").descending()))
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
        return OperationCatalogResourceResponse(requireNotNull(categoryRepository.save(category).publicId))
    }

    fun createBrand(request: CreateBrandRequest): OperationCatalogResourceResponse {
        validateDisplayStatus(request.displayStatus)
        val name = request.name.trim()
        require(!brandRepository.existsByName(name)) { "이미 존재하는 브랜드입니다." }
        val brand = BrandEntity().apply { this.name = name; displayStatus = request.displayStatus }
        return OperationCatalogResourceResponse(requireNotNull(brandRepository.save(brand).publicId))
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
        val savedProduct = productRepository.save(product)
        request.skus.forEach { sku -> productSkuRepository.save(skuEntity(savedProduct, sku)) }
        return OperationCatalogResourceResponse(requireNotNull(savedProduct.publicId))
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
        require(!productSkuRepository.existsBySkuCode(request.skuCode)) { "이미 존재하는 SKU 코드입니다." }
        return ProductSkuEntity().apply {
            this.product = product
            skuCode = request.skuCode.trim()
            name = request.name.trim()
            salePrice = request.salePrice
            listPrice = request.listPrice
            salesStatus = request.salesStatus
        }
    }

    private fun category(categoryId: UUID): CategoryEntity =
        categoryRepository.findByPublicIdAndDeletedAtIsNull(categoryId) ?: throw ItemNotFoundException("카테고리를 찾을 수 없습니다.")

    private fun brand(brandId: UUID): BrandEntity =
        brandRepository.findByPublicIdAndDeletedAtIsNull(brandId) ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")

    private fun product(productId: UUID): ProductEntity =
        productRepository.findByPublicIdAndDeletedAtIsNull(productId) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")

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
                    productSkuRepository.findAllByProductIdOrderBySalePriceAsc(requireNotNull(product.id)).map { sku ->
                        OperationProductSkuResponse(requireNotNull(sku.publicId), sku.skuCode, sku.name, sku.salePrice, sku.listPrice, sku.salesStatus)
                    }
                } else {
                    emptyList()
                },
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
