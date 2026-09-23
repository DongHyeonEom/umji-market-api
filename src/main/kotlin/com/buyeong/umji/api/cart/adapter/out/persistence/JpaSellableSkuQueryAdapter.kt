package com.buyeong.umji.api.cart.adapter.out.persistence

import com.buyeong.umji.api.cart.application.model.SellableSku
import com.buyeong.umji.api.cart.application.port.out.SellableSkuQueryPort
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaSellableSkuQueryAdapter(private val catalog: CatalogJpaEntityService) : SellableSkuQueryPort {
    @Transactional(readOnly = true)
    override fun find(skuId: UUID): SellableSku? = catalog.sku(skuId)?.let {
        SellableSku(requireNotNull(it.publicId), it.skuCode, it.product.name, it.name, it.salePrice, it.salesStatus)
    }
}
