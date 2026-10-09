package com.buyeong.umji.api.persistence.jpa.catalog.service

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductImageEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductOptionEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductOptionValueEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.BrandRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.CategoryRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ChannelProductListingRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductImageRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductOptionRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductOptionValueRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ProductSkuRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CatalogJpaEntityService(
    private val categories: CategoryRepository,
    private val brands: BrandRepository,
    private val products: ProductRepository,
    private val skus: ProductSkuRepository,
    private val images: ProductImageRepository,
    private val options: ProductOptionRepository,
    private val optionValues: ProductOptionValueRepository,
    private val listings: ChannelProductListingRepository,
    private val offers: SalesOfferRepository,
) {
    fun displayedCategories(
        channelCode: String,
        status: String,
    ): List<CategoryEntity> = categories.findAllBySalesChannel_CodeAndDisplayStatusAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(channelCode, status)
    fun categories(channelCode: String): List<CategoryEntity> = categories.findAllBySalesChannel_CodeAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(channelCode)
    fun category(id: UUID): CategoryEntity? = categories.findByPublicIdAndDeletedAtIsNull(id)
    fun brands(pageable: Pageable): Page<BrandEntity> = brands.findAllByDeletedAtIsNull(pageable)
    fun brand(id: UUID): BrandEntity? = brands.findByPublicIdAndDeletedAtIsNull(id)
    fun existsBrandName(name: String): Boolean = brands.existsByOrganizationIsNullAndName(name)
    fun sellerBrands(organizationId: UUID, pageable: Pageable) = brands.findAllByOrganization_PublicIdAndDeletedAtIsNull(organizationId, pageable)
    fun sellerBrandExists(organizationId: UUID, name: String) = brands.existsByOrganization_PublicIdAndNameAndDeletedAtIsNull(organizationId, name)
    fun publicProducts(
        displayStatus: String,
        salesStatus: String,
        pageable: Pageable,
    ): Page<ProductEntity> = products.findAllByDisplayStatusAndSalesStatusAndDeletedAtIsNull(displayStatus, salesStatus, pageable)
    fun publicProduct(
        id: UUID,
        displayStatus: String,
        salesStatus: String,
    ): ProductEntity? = products.findByPublicIdAndDisplayStatusAndSalesStatusAndDeletedAtIsNull(id, displayStatus, salesStatus)
    fun products(pageable: Pageable): Page<ProductEntity> = products.findAllByDeletedAtIsNull(pageable)
    fun product(id: UUID): ProductEntity? = products.findByPublicIdAndDeletedAtIsNull(id)
    fun sellerProducts(organizationId: UUID, pageable: Pageable) = products.findAllByOrganization_PublicIdAndDeletedAtIsNull(organizationId, pageable)
    fun sellerProduct(id: UUID, organizationId: UUID): ProductEntity? = products.findByPublicIdAndOrganization_PublicIdAndDeletedAtIsNull(id, organizationId)
    fun sku(id: UUID): ProductSkuEntity? = skus.findByPublicId(id)
    fun skus(productId: Long, salesStatus: String): List<ProductSkuEntity> = skus.findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId, salesStatus)
    fun skus(productId: Long): List<ProductSkuEntity> = skus.findAllByProductIdOrderBySalePriceAsc(productId)
    fun existsSkuCode(productId: Long, code: String): Boolean = skus.existsByProduct_IdAndSkuCode(productId, code)
    fun images(productId: Long): List<ProductImageEntity> = images.findAllByProductIdOrderByDisplayOrderAscIdAsc(productId)
    fun options(productId: Long): List<ProductOptionEntity> = options.findAllByProductIdOrderByDisplayOrderAscIdAsc(productId)
    fun optionValues(optionId: Long): List<ProductOptionValueEntity> = optionValues.findAllByOptionIdOrderByDisplayOrderAscIdAsc(optionId)
    fun optionValues(ids: Collection<UUID>): List<ProductOptionValueEntity> = optionValues.findAllByPublicIdIn(ids)
    fun channelCategories(channelCode: String, status: String) = listings.findCategories(channelCode, status)
    fun publicListings(channelCode: String, displayStatus: String, salesStatus: String, page: Int, size: Int) =
        listings.findPublicListings(channelCode, displayStatus, salesStatus, PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("id").descending())))
    fun publicListing(channelCode: String, productId: UUID, displayStatus: String, salesStatus: String) =
        listings.findPublicListing(channelCode, productId, displayStatus, salesStatus)
    fun activeSalesOffers(channelCode: String, salesStatus: String, productId: Long) =
        offers.findAllBySalesChannel_CodeAndSalesStatusAndProductSku_Product_IdOrderBySalePriceAsc(channelCode, salesStatus, productId)
    fun salesOffer(id: UUID): SalesOfferEntity? = offers.findByPublicId(id)
    fun salesOffer(channelCode: String, skuId: UUID): SalesOfferEntity? =
        offers.findFirstBySalesChannel_CodeAndProductSku_PublicIdAndSalesStatusOrderBySalePriceAsc(channelCode, skuId, "ON_SALE")
    fun salesOffer(channelCode: String, skuId: UUID, organizationPublicId: UUID): SalesOfferEntity? =
        offers.findBySalesChannel_CodeAndProductSku_PublicIdAndOrganization_PublicId(channelCode, skuId, organizationPublicId)
    fun sellerSkus(organizationId: UUID, pageable: Pageable) = skus.findAllByProduct_Organization_PublicIdAndProduct_DeletedAtIsNull(organizationId, pageable)

    @Transactional fun save(category: CategoryEntity): CategoryEntity = categories.save(category)

    @Transactional fun save(brand: BrandEntity): BrandEntity = brands.save(brand)

    @Transactional fun save(product: ProductEntity): ProductEntity = products.save(product)

    @Transactional fun save(sku: ProductSkuEntity): ProductSkuEntity = skus.save(sku)

    @Transactional fun save(image: ProductImageEntity): ProductImageEntity = images.save(image)

    @Transactional fun save(option: ProductOptionEntity): ProductOptionEntity = options.save(option)

    @Transactional fun save(value: ProductOptionValueEntity): ProductOptionValueEntity = optionValues.save(value)
}