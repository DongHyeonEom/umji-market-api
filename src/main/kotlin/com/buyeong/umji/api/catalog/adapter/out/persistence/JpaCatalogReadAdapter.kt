package com.buyeong.umji.api.catalog.adapter.out.persistence

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import com.buyeong.umji.api.catalog.application.model.ProductSkuView
import com.buyeong.umji.api.catalog.application.model.ProductSummaryView
import com.buyeong.umji.api.catalog.application.port.out.CatalogReadPort
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ChannelProductListingRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaCatalogReadAdapter(
    private val catalog: CatalogJpaEntityService,
    private val listings: ChannelProductListingRepository,
    private val offers: SalesOfferRepository,
) : CatalogReadPort {
    @Transactional(readOnly = true)
    override fun categories(channelCode: String): List<CategoryView> = listings.findCategories(channelCode, DISPLAYED).map { category ->
        CategoryView(requireNotNull(category.publicId), category.name, category.path, category.depth, channelCode)
    }

    @Transactional(readOnly = true)
    override fun products(page: Int, size: Int, channelCode: String): ProductPageView {
        val result = listings.findPublicListings(
            channelCode,
            DISPLAYED,
            ON_SALE,
            PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("id").descending())),
        )
        return ProductPageView(
            items = result.content.map { listing ->
                val prices = offers.findAllBySalesChannel_CodeAndSalesStatusAndProductSku_Product_IdOrderBySalePriceAsc(
                    channelCode,
                    ON_SALE,
                    requireNotNull(listing.product.id),
                )
                ProductSummaryView(
                    requireNotNull(listing.product.publicId),
                    listing.product.name,
                    listing.product.brand?.name,
                    channelCode,
                    prices.firstOrNull()?.salePrice,
                )
            },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional(readOnly = true)
    override fun product(channelCode: String, productId: UUID): ProductDetailView? {
        val listing = listings.findPublicListing(channelCode, productId, DISPLAYED, ON_SALE) ?: return null
        val product = listing.product
        val salesOffers = offers.findAllBySalesChannel_CodeAndSalesStatusAndProductSku_Product_IdOrderBySalePriceAsc(
            channelCode,
            ON_SALE,
            requireNotNull(product.id),
        )
        return ProductDetailView(
            id = requireNotNull(product.publicId),
            name = product.name,
            description = product.description,
            categoryName = listing.category.name,
            brandName = product.brand?.name,
            skus = salesOffers.map { offer ->
                val sku = offer.productSku
                ProductSkuView(
                    requireNotNull(sku.publicId),
                    sku.skuCode,
                    sku.name,
                    offer.salePrice,
                    offer.listPrice,
                    requireNotNull(offer.publicId),
                )
            },
            channelCode = channelCode,
        )
    }

    private companion object {
        const val DISPLAYED = "DISPLAYED"
        const val ON_SALE = "ON_SALE"
    }
}
