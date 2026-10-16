package com.buyeong.umji.api.domain.catalog.service

import com.buyeong.umji.api.domain.catalog.dto.CategoryViewDto
import com.buyeong.umji.api.domain.catalog.dto.ProductDetailViewDto
import com.buyeong.umji.api.domain.catalog.dto.ProductPageViewDto
import com.buyeong.umji.api.domain.catalog.dto.ProductSkuViewDto
import com.buyeong.umji.api.domain.catalog.dto.ProductSummaryViewDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogService(
    private val catalog: CatalogJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun categories(accountId: UUID, requestedChannel: String?): List<CategoryViewDto> {
        val channel = allowedChannel(accountId, requestedChannel)
        return catalog.channelCategories(channel, DISPLAYED).map {
            CategoryViewDto(requireNotNull(it.publicId), it.name, it.path, it.depth, channel)
        }
    }

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int, accountId: UUID, requestedChannel: String?): ProductPageViewDto {
        val channelCode = allowedChannel(accountId, requestedChannel)
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
    fun product(productId: UUID, accountId: UUID, requestedChannel: String?): ProductDetailViewDto {
        val channel = allowedChannel(accountId, requestedChannel)
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

    private fun allowedChannel(accountId: UUID, requestedChannel: String?): String {
        val allowed = organizations.allowedSalesChannelForAccountPublicId(accountId)
        val requested = requestedChannel?.let(::channel)
        require(requested == null || requested == allowed) { "구매 Organization에 허용된 판매 채널이 아닙니다." }
        return allowed
    }

    private companion object {
        const val DISPLAYED = "DISPLAYED"
        const val ON_SALE = "ON_SALE"
    }
}
