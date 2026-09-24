package com.buyeong.umji.api.catalog.adapter.out.persistence

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import com.buyeong.umji.api.catalog.application.model.ProductSkuView
import com.buyeong.umji.api.catalog.application.model.ProductSummaryView
import com.buyeong.umji.api.catalog.application.port.out.CatalogReadPort
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaCatalogReadAdapter(private val catalog: CatalogJpaEntityService) : CatalogReadPort {
    @Transactional(readOnly = true)
    override fun categories(): List<CategoryView> = catalog.displayedCategories(DISPLAYED).map { category ->
        CategoryView(requireNotNull(category.publicId), category.name, category.path, category.depth)
    }

    @Transactional(readOnly = true)
    override fun products(page: Int, size: Int): ProductPageView {
        val result = catalog.publicProducts(
            DISPLAYED,
            ON_SALE,
            PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("id").descending())),
        )
        return ProductPageView(
            items = result.content.map { ProductSummaryView(requireNotNull(it.publicId), it.name, it.brand?.name) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    override fun product(productId: UUID): ProductDetailView? {
        val product = catalog.publicProduct(productId, DISPLAYED, ON_SALE) ?: return null
        return ProductDetailView(
            id = requireNotNull(product.publicId),
            name = product.name,
            description = product.description,
            categoryName = product.category.name,
            brandName = product.brand?.name,
            skus = catalog.skus(requireNotNull(product.id), ON_SALE).map {
                ProductSkuView(requireNotNull(it.publicId), it.skuCode, it.name, it.salePrice, it.listPrice)
            },
        )
    }

    private companion object {
        const val DISPLAYED = "DISPLAYED"
        const val ON_SALE = "ON_SALE"
    }
}