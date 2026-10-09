package com.buyeong.umji.api.catalog.service

import com.buyeong.umji.api.catalog.dto.CategoryViewDto
import com.buyeong.umji.api.catalog.dto.ProductDetailViewDto
import com.buyeong.umji.api.catalog.dto.ProductPageViewDto
import com.buyeong.umji.api.catalog.dto.ProductSkuViewDto
import com.buyeong.umji.api.catalog.dto.ProductSummaryViewDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogService(private val catalog: CatalogJpaEntityService) {
    @Transactional(readOnly = true)
    fun categories(channelCode: String): List<CategoryViewDto> = catalog.channelCategories(channel(channelCode), DISPLAYED).map {
        CategoryViewDto(requireNotNull(it.publicId), it.name, it.path, it.depth, channelCode)
    }

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int, channelCode: String): ProductPageViewDto {
        val result = catalog.publicListings(channel(channelCode), DISPLAYED, ON_SALE, page, size)
        return ProductPageViewDto(
            result.content.map { listing ->
                val offers = catalog.activeSalesOffers(channelCode, ON_SALE, requireNotNull(listing.product.id))
                ProductSummaryViewDto(
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
    fun product(productId: UUID, channelCode: String): ProductDetailViewDto {
        val channel = channel(channelCode)
        val listing = catalog.publicListing(channel, productId, DISPLAYED, ON_SALE)
            ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
        val offers = catalog.activeSalesOffers(channel, ON_SALE, requireNotNull(listing.product.id))
        return ProductDetailViewDto(
            id = requireNotNull(listing.product.publicId),
            name = listing.product.name,
            description = listing.product.description,
            categoryName = listing.category.name,
            brandName = listing.product.brand?.name,
            skus = offers.map { offer ->
                ProductSkuViewDto(
                    requireNotNull(offer.productSku.publicId),
                    offer.productSku.skuCode,
                    offer.productSku.name,
                    offer.salePrice,
                    offer.listPrice,
                    requireNotNull(offer.publicId),
                    offer.unitsPerSale,
                    offer.organization?.publicId,
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