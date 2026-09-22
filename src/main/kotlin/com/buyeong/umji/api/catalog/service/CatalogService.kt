package com.buyeong.umji.api.catalog.service

import com.buyeong.umji.api.catalog.model.CategoryResponse
import com.buyeong.umji.api.catalog.model.ProductDetailResponse
import com.buyeong.umji.api.catalog.model.ProductPageResponse
import com.buyeong.umji.api.catalog.model.ProductSkuResponse
import com.buyeong.umji.api.catalog.model.ProductSummaryResponse
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CatalogService(private val catalog: CatalogJpaEntityService) {
    fun categories(): List<CategoryResponse> =
        catalog.displayedCategories(DISPLAYED)
            .map { category ->
                CategoryResponse(
                    id = requireNotNull(category.publicId),
                    name = category.name,
                    path = category.path,
                    depth = category.depth,
                )
            }

    fun products(page: Int, size: Int): ProductPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("id").descending()))
        val products = catalog.publicProducts(DISPLAYED, ON_SALE, pageable)
        return ProductPageResponse(
            items = products.content.map { product -> ProductSummaryResponse(requireNotNull(product.publicId), product.name, product.brand?.name) },
            page = products.number,
            size = products.size,
            totalElements = products.totalElements,
            totalPages = products.totalPages,
        )
    }

    fun product(productId: UUID): ProductDetailResponse {
        val product =
            catalog.publicProduct(productId, DISPLAYED, ON_SALE)
                ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
        val skus =
            catalog.skus(requireNotNull(product.id), ON_SALE).map { sku ->
                ProductSkuResponse(requireNotNull(sku.publicId), sku.skuCode, sku.name, sku.salePrice, sku.listPrice)
            }
        return ProductDetailResponse(
            id = requireNotNull(product.publicId),
            name = product.name,
            description = product.description,
            categoryName = product.category.name,
            brandName = product.brand?.name,
            skus = skus,
        )
    }

    private companion object {
        const val DISPLAYED = "DISPLAYED"
        const val ON_SALE = "ON_SALE"
    }
}
