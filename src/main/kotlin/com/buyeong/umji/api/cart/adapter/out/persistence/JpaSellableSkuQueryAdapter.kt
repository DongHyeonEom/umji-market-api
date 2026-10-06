package com.buyeong.umji.api.cart.adapter.out.persistence

import com.buyeong.umji.api.cart.application.model.SellableSku
import com.buyeong.umji.api.cart.application.port.out.SellableSkuQueryPort
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaSellableSkuQueryAdapter(private val catalog: CatalogJpaEntityService, private val offers: SalesOfferRepository) : SellableSkuQueryPort {
    @Transactional(readOnly = true)
    override fun find(skuId: UUID, channelCode: String): SellableSku? = catalog.sku(skuId)?.let { sku ->
        offers.findBySalesChannel_CodeAndProductSku_PublicId(channelCode.uppercase(), skuId)?.toSellable()
    }

    @Transactional(readOnly = true)
    override fun findOffer(salesOfferId: UUID): SellableSku? = offers.findByPublicId(salesOfferId)?.toSellable()

    private fun com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity.toSellable() = SellableSku(
        requireNotNull(productSku.publicId),
        requireNotNull(publicId),
        salesChannel.code,
        productSku.skuCode,
        productSku.product.name,
        productSku.name,
        salePrice,
        salesStatus,
    )
}
