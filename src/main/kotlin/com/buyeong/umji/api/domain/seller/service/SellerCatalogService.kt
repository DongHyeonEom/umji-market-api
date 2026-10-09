package com.buyeong.umji.api.domain.seller.service

import com.buyeong.umji.api.domain.account.service.OrganizationMembershipService
import com.buyeong.umji.api.domain.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.operation.catalog.dto.BrandCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ChannelListingCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferCommandDto
import com.buyeong.umji.api.domain.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.domain.seller.dto.SellerSkuResponseDto
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
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
    private val businessProfiles: OrganizationTaxInvoiceProfileService,
) {
    @Transactional(readOnly = true)
    fun skus(accountId: UUID, page: Int, size: Int) = sellerOrganization(accountId).let {
        commonCatalog.sellerSkus(
            it,
            PageRequest.of(page, size, Sort.by("product.name").ascending().and(Sort.by("skuCode").ascending())),
        ).map { sku ->
            SellerSkuResponseDto(
                requireNotNull(sku.publicId),
                sku.skuCode,
                sku.name,
                requireNotNull(sku.product.publicId),
                sku.product.name,
                sku.product.brand?.name,
            )
        }
    }

    @Transactional(readOnly = true)
    fun brands(accountId: UUID, page: Int, size: Int) = catalog.sellerBrands(sellerOrganization(accountId), page, size)

    @Transactional
    fun createBrand(accountId: UUID, name: String, displayStatus: String) =
        catalog.createSellerBrand(sellerOrganization(accountId), BrandCommandDto(name, displayStatus))

    @Transactional(readOnly = true)
    fun products(accountId: UUID, page: Int, size: Int) = catalog.sellerProducts(sellerOrganization(accountId), page, size)

    @Transactional(readOnly = true)
    fun product(accountId: UUID, productId: UUID) = catalog.sellerProduct(sellerOrganization(accountId), productId)

    @Transactional
    fun createProduct(accountId: UUID, command: ProductCommandDto) = catalog.createSellerProduct(sellerOrganization(accountId), command)

    @Transactional
    fun updateProduct(accountId: UUID, productId: UUID, command: ProductCommandDto) =
        catalog.updateSellerProduct(sellerOrganization(accountId), productId, command)

    @Transactional
    fun updateListing(accountId: UUID, channelCode: String, productId: UUID, categoryId: UUID, displayStatus: String, displayOrder: Int) =
        catalog.updateSellerChannelListing(sellerOrganization(accountId), ChannelListingCommandDto(channelCode, productId, categoryId, displayStatus, displayOrder))

    @Transactional
    fun updateOffer(accountId: UUID, channelCode: String, skuId: UUID, salePrice: Long, listPrice: Long?, salesStatus: String, unitsPerSale: Int?) =
        sellerOrganization(accountId).let { organizationId ->
            if (salesStatus == "ON_SALE" && !businessProfiles.isSellerBusinessProfileReady(organizationId)) {
                throw ClientBadRequestException("판매하려면 확인이 완료된 사업자 Organization 프로필이 필요합니다.")
            }
            verifyOwnedSku(organizationId, skuId)
            catalog.updateSellerSalesOffer(
                organizationId,
                SalesOfferCommandDto(channelCode, skuId, salePrice, listPrice, salesStatus, unitsPerSale),
            )
        }

    @Transactional(readOnly = true)
    fun stock(accountId: UUID, skuId: UUID) = sellerOrganization(accountId).also { verifyOwnedSku(it, skuId) }.let { inventory.stock(skuId, it) }

    @Transactional
    fun adjustStock(accountId: UUID, skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?) =
        sellerOrganization(accountId).also { verifyOwnedSku(it, skuId) }.let { inventory.adjust(skuId, quantityDelta, reason, memo, safetyStock, it) }

    @Transactional(readOnly = true)
    fun movements(accountId: UUID, skuId: UUID, page: Int, size: Int) =
        sellerOrganization(accountId).also { verifyOwnedSku(it, skuId) }.let { inventory.movements(skuId, page, size, it) }

    private fun verifyOwnedSku(organizationId: UUID, skuId: UUID) {
        val sku = commonCatalog.sku(skuId) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
        if (sku.product.organization?.publicId != organizationId) throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    }

    private fun sellerOrganization(accountId: UUID): UUID {
        val organization = organizations.current(accountId) ?: throw ItemNotFoundException("활성 Organization을 찾을 수 없습니다.")
        require("SELLER" in organization.capabilities) { "판매 Organization 기능이 필요합니다." }
        return organization.id
    }
}