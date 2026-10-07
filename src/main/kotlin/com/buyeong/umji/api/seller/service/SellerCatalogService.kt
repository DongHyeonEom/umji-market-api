package com.buyeong.umji.api.seller.service

import com.buyeong.umji.api.account.service.OrganizationMembershipService
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.catalog.model.SalesOfferCommand
import com.buyeong.umji.api.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SellerCatalogService(
    private val organizations: OrganizationMembershipService,
    private val catalog: OperationCatalogService,
    private val inventory: InventoryService,
    private val commonCatalog: CatalogJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun skus(accountId: UUID, page: Int, size: Int) = sellerOrganization(accountId).let {
        commonCatalog.sellableSkus(
            PageRequest.of(page, size, Sort.by("product.name").ascending().and(Sort.by("skuCode").ascending())),
        ).map { sku ->
            SellerSkuResponseView(
                requireNotNull(sku.publicId), sku.skuCode, sku.name,
                requireNotNull(sku.product.publicId), sku.product.name, sku.product.brand?.name,
            )
        }
    }

    @Transactional
    fun updateOffer(accountId: UUID, channelCode: String, skuId: UUID, salePrice: Long, listPrice: Long?, salesStatus: String, unitsPerSale: Int?) =
        catalog.updateSellerSalesOffer(
            sellerOrganization(accountId),
            SalesOfferCommand(channelCode, skuId, salePrice, listPrice, salesStatus, unitsPerSale),
        )

    @Transactional(readOnly = true)
    fun stock(accountId: UUID, skuId: UUID) = inventory.stock(skuId, sellerOrganization(accountId))

    @Transactional
    fun adjustStock(accountId: UUID, skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?) =
        inventory.adjust(skuId, quantityDelta, reason, memo, safetyStock, sellerOrganization(accountId))

    @Transactional(readOnly = true)
    fun movements(accountId: UUID, skuId: UUID, page: Int, size: Int) =
        inventory.movements(skuId, page, size, sellerOrganization(accountId))

    private fun sellerOrganization(accountId: UUID): UUID {
        val organization = organizations.current(accountId) ?: throw ItemNotFoundException("활성 Organization을 찾을 수 없습니다.")
        require("SELLER" in organization.capabilities) { "판매 Organization 기능이 필요합니다." }
        return organization.id
    }
}

data class SellerSkuResponseView(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val productId: UUID,
    val productName: String,
    val brandName: String?,
)
