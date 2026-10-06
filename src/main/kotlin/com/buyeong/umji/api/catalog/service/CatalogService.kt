package com.buyeong.umji.api.catalog.service

import com.buyeong.umji.api.catalog.model.CategoryView
import com.buyeong.umji.api.catalog.model.ProductDetailView
import com.buyeong.umji.api.catalog.model.ProductPageView
import com.buyeong.umji.api.catalog.model.ProductSkuView
import com.buyeong.umji.api.catalog.model.ProductSummaryView
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogService(private val catalog: CatalogJpaEntityService) {
    @Transactional(readOnly = true)
    fun categories(channelCode: String): List<CategoryView> = catalog.channelCategories(channel(channelCode), DISPLAYED).map {
        CategoryView(requireNotNull(it.publicId), it.name, it.path, it.depth, channelCode)
    }

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int, channelCode: String): ProductPageView {
        val result = catalog.publicListings(channel(channelCode), DISPLAYED, ON_SALE, page, size)
        return ProductPageView(
            result.content.map { listing ->
                val offers = catalog.activeSalesOffers(channelCode, ON_SALE, requireNotNull(listing.product.id))
                ProductSummaryView(
                    requireNotNull(listing.product.publicId),
                    listing.product.name,
                    listing.product.brand?.name,
                    channelCode,
                    offers.firstOrNull()?.salePrice,
                    offers.firstOrNull()?.unitsPerSale ?: 1,
                )
            },
            result.number,
            result.size,
            result.totalElements,
            result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun product(productId: UUID, channelCode: String): ProductDetailView {
        val channel = channel(channelCode)
        val listing = catalog.publicListing(channel, productId, DISPLAYED, ON_SALE)
            ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
        val offers = catalog.activeSalesOffers(channel, ON_SALE, requireNotNull(listing.product.id))
        return ProductDetailView(
            id = requireNotNull(listing.product.publicId),
            name = listing.product.name,
            description = listing.product.description,
            categoryName = listing.category.name,
            brandName = listing.product.brand?.name,
            skus = offers.map { offer ->
                ProductSkuView(
                    requireNotNull(offer.productSku.publicId),
                    offer.productSku.skuCode,
                    offer.productSku.name,
                    offer.salePrice,
                    offer.listPrice,
                    requireNotNull(offer.publicId),
                    offer.unitsPerSale,
                )
            },
            channelCode = channel,
        )
    }

    private fun channel(code: String) = code.uppercase().also {
        require(it in setOf("WHOLESALE", "RETAIL")) { "유효하지 않은 판매 채널입니다." }
    }

    private companion object {
        const val DISPLAYED = "DISPLAYED"
        const val ON_SALE = "ON_SALE"
    }
}
