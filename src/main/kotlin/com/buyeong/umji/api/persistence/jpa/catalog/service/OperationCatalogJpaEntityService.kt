package com.buyeong.umji.api.persistence.jpa.catalog.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.catalog.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.model.BrandView
import com.buyeong.umji.api.operation.catalog.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.model.CategoryView
import com.buyeong.umji.api.operation.catalog.model.ChannelCategoryCommand
import com.buyeong.umji.api.operation.catalog.model.ChannelListingCommand
import com.buyeong.umji.api.operation.catalog.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.model.ImageView
import com.buyeong.umji.api.operation.catalog.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.model.OptionValueView
import com.buyeong.umji.api.operation.catalog.model.OptionView
import com.buyeong.umji.api.operation.catalog.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.model.ProductPageView
import com.buyeong.umji.api.operation.catalog.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.model.ProductView
import com.buyeong.umji.api.operation.catalog.model.SalesOfferCommand
import com.buyeong.umji.api.operation.catalog.model.SalesOfferView
import com.buyeong.umji.api.operation.catalog.model.SkuCommand
import com.buyeong.umji.api.operation.catalog.model.SkuView
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ChannelProductListingEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductImageEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductOptionEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductOptionValueEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ChannelProductListingRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesChannelRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OperationCatalogJpaEntityService(
    private val catalog: CatalogJpaEntityService,
    private val channels: SalesChannelRepository,
    private val listings: ChannelProductListingRepository,
    private val offers: SalesOfferRepository,
    private val organizations: OrganizationRepository,
) {
    @Transactional(readOnly = true)
    fun categories(channelCode: String) = catalog.categories(channelCode).map {
        CategoryView(requireNotNull(it.publicId), it.parent?.publicId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus)
    }

    @Transactional(readOnly = true)
    fun brands(
        page: Int,
        size: Int,
    ) = catalog.brands(PageRequest.of(page, size, Sort.by("name"))).content.map { BrandView(requireNotNull(it.publicId), it.name, it.displayStatus) }

    @Transactional(readOnly = true)
    fun products(
        page: Int,
        size: Int,
    ): ProductPageView {
        val p = catalog.products(PageRequest.of(page, size, Sort.by("id").descending()))
        return ProductPageView(p.content.map { it.toView() }, p.number, p.size, p.totalElements, p.totalPages)
    }

    @Transactional(readOnly = true)
    fun product(id: UUID) = catalog.product(id)?.toView(true)

    fun createCategory(command: CategoryCommand): CatalogResource {
        val parent = command.parentId?.let(::category)
        val e = CategoryEntity().apply {
            name = command.name
            this.parent =
                parent
            depth = (parent?.depth ?: -1) + 1
            path = parent?.path?.let { "$it/$name" } ?: name
            displayOrder = command.displayOrder
            displayStatus = command.displayStatus
            salesChannel = channel(WHOLESALE)
        }
        return CatalogResource(requireNotNull(catalog.save(e).publicId))
    }

    fun createChannelCategory(command: ChannelCategoryCommand): CatalogResource {
        val salesChannel = channel(command.channelCode)
        val parent = command.parentId?.let(::category)
        require(parent == null || parent.salesChannel.id == salesChannel.id) { "상위 카테고리는 같은 판매 채널에 속해야 합니다." }
        val entity = CategoryEntity().apply {
            this.salesChannel = salesChannel
            name = command.name
            this.parent = parent
            depth = (parent?.depth ?: -1) + 1
            path = parent?.path?.let { "$it/${command.name}" } ?: command.name
            displayOrder = command.displayOrder
            displayStatus = command.displayStatus
        }
        return CatalogResource(requireNotNull(catalog.save(entity).publicId))
    }

    fun updateChannelListing(command: ChannelListingCommand): CatalogResource? {
        val channel = channel(command.channelCode)
        val product = catalog.product(command.productId) ?: return null
        val category = category(command.categoryId)
        require(category.salesChannel.id == channel.id) { "상품 listing 카테고리는 같은 판매 채널에 속해야 합니다." }
        val listing = listings.findBySalesChannel_IdAndProduct_Id(requireNotNull(channel.id), requireNotNull(product.id))
            ?: ChannelProductListingEntity().apply {
                salesChannel = channel
                this.product = product
            }
        listing.category = category
        listing.displayStatus = command.displayStatus
        listing.displayOrder = command.displayOrder
        return CatalogResource(requireNotNull(listings.save(listing).publicId))
    }

    fun updateSalesOffer(command: SalesOfferCommand): SalesOfferView? {
        require(command.unitsPerSale == null || command.unitsPerSale > 0) { "판매 단위 입수 수량은 1 이상이어야 합니다." }
        val channel = channel(command.channelCode)
        val sku = catalog.sku(command.skuId) ?: return null
        val offer = offers.findFirstBySalesChannel_IdAndProductSku_IdAndOrganizationIsNull(requireNotNull(channel.id), requireNotNull(sku.id))
            ?: return null
        offer.salePrice = command.salePrice
        offer.listPrice = command.listPrice
        offer.salesStatus = command.salesStatus
        offer.unitsPerSale = command.unitsPerSale ?: offer.unitsPerSale
        require(command.channelCode != RETAIL || offer.unitsPerSale == 1) { "RETAIL 오퍼의 판매 단위 입수 수량은 1이어야 합니다." }
        return offers.save(offer).toView()
    }

    fun updateSellerSalesOffer(organizationPublicId: UUID, command: SalesOfferCommand): SalesOfferView? {
        require(command.unitsPerSale == null || command.unitsPerSale > 0) { "판매 단위 입수 수량은 1 이상이어야 합니다." }
        val organization = organizations.findByPublicId(organizationPublicId) ?: return null
        val channel = channel(command.channelCode)
        val sku = catalog.sku(command.skuId) ?: return null
        require(sku.salesStatus == ON_SALE && sku.product.salesStatus == ON_SALE && sku.product.displayStatus == DISPLAYED) {
            "판매 가능한 공용 SKU만 오퍼로 등록할 수 있습니다."
        }
        val offer = offers.findBySalesChannel_IdAndProductSku_IdAndOrganization_Id(
            requireNotNull(channel.id), requireNotNull(sku.id), requireNotNull(organization.id),
        ) ?: SalesOfferEntity().apply {
            salesChannel = channel
            productSku = sku
            this.organization = organization
        }
        offer.salePrice = command.salePrice
        offer.listPrice = command.listPrice
        offer.salesStatus = command.salesStatus
        offer.unitsPerSale = command.unitsPerSale ?: offer.unitsPerSale
        require(command.channelCode != RETAIL || offer.unitsPerSale == 1) { "RETAIL 오퍼의 판매 단위 입수 수량은 1이어야 합니다." }
        return offers.save(offer).toView()
    }
    fun createBrand(
        command: BrandCommand,
    ): CatalogResource {
        require(!catalog.existsBrandName(command.name)) { "이미 존재하는 브랜드입니다." }
        return CatalogResource(
            requireNotNull(
                catalog.save(
                    BrandEntity().apply {
                        name =
                            command.name
                        displayStatus = command.displayStatus
                    },
                ).publicId,
            ),
        )
    }
    fun createProduct(
        command: ProductCommand,
    ): CatalogResource {
        val product = buildProduct(command)
        val saved = catalog.save(product)
        wholesaleListing(saved, saved.category, saved.displayStatus, saved.displayOrder)
        command.images.forEach { catalog.save(image(saved, it)) }
        command.options.forEach { saveOption(saved, it) }
        command.skus.forEach {
            catalog.save(sku(saved, it))
        }
        return CatalogResource(requireNotNull(saved.publicId))
    }
    fun updateProduct(id: UUID, command: ProductCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        applyProduct(p, command)
        wholesaleListing(p, p.category, p.displayStatus, p.displayOrder)
        return CatalogResource(requireNotNull(p.publicId))
    }
    fun addImage(id: UUID, command: ImageCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        return CatalogResource(requireNotNull(catalog.save(image(p, command)).publicId))
    }
    fun addOption(id: UUID, command: OptionCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        return CatalogResource(requireNotNull(saveOption(p, command).publicId))
    }
    fun addSku(id: UUID, command: SkuCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        val savedSku = catalog.save(sku(p, command))
        return CatalogResource(requireNotNull(savedSku.publicId))
    }
    fun updateStatus(id: UUID, command: ProductStatusCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        p.displayStatus = command.displayStatus
        p.salesStatus = command.salesStatus
        wholesaleListing(p, p.category, command.displayStatus, p.displayOrder)
        offers.findAllBySalesChannel_IdAndProductSku_Product_Id(requireNotNull(channel(WHOLESALE).id), requireNotNull(p.id))
            .forEach { it.salesStatus = command.salesStatus }
        return CatalogResource(requireNotNull(p.publicId))
    }

    private fun buildProduct(c: ProductCommand) = ProductEntity().also { applyProduct(it, c) }
    private fun applyProduct(
        p: ProductEntity,
        c: ProductCommand,
    ) {
        p.category = category(c.categoryId)
        p.brand = c.brandId?.let(::brand)
        p.name = c.name
        p.description = c.description
        p.displayStatus =
            c.displayStatus
        p.salesStatus = c.salesStatus
        p.displayOrder = c.displayOrder
    }
    private fun image(p: ProductEntity, c: ImageCommand) = ProductImageEntity().apply {
        product = p
        storageKey = c.storageKey
        altText = c.altText
        displayOrder = c.displayOrder
    }
    private fun saveOption(
        p: ProductEntity,
        c: OptionCommand,
    ): ProductOptionEntity {
        val option = catalog.save(
            ProductOptionEntity().apply {
                product = p
                name = c.name
                displayOrder =
                    c.displayOrder
            },
        )
        c.values.forEach { v ->
            catalog.save(
                ProductOptionValueEntity().apply {
                    this.option = option
                    value = v.value
                    displayOrder = v.displayOrder
                },
            )
        }
        return option
    }
    private fun sku(
        p: ProductEntity,
        c: SkuCommand,
    ): ProductSkuEntity {
        require(!catalog.existsSkuCode(c.skuCode)) { "이미 존재하는 SKU 코드입니다." }
        val values = if (c.optionValueIds.isEmpty()) {
            linkedSetOf()
        } else {
            catalog.optionValues(c.optionValueIds).also { found ->
                require(
                    found.size == c.optionValueIds.size && found.all { it.option.product.id == p.id },
                ) { "상품에 속하지 않는 옵션값이 포함되어 있습니다." }
            }.toMutableSet()
        }
        return ProductSkuEntity().apply {
            product =
                p
            skuCode = c.skuCode
            name = c.name
            salePrice = c.salePrice
            listPrice = c.listPrice
            salesStatus = c.salesStatus
            optionValues = values
        }
    }
    private fun category(id: UUID) = catalog.category(id) ?: throw ItemNotFoundException("카테고리를 찾을 수 없습니다.")
    private fun brand(id: UUID) = catalog.brand(id) ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")
    private fun channel(code: String): SalesChannelEntity = channels.findByCode(code) ?: throw ItemNotFoundException("판매 채널을 찾을 수 없습니다.")

    private fun wholesaleListing(product: ProductEntity, category: CategoryEntity, displayStatus: String, displayOrder: Int) {
        val salesChannel = channel(WHOLESALE)
        val listing = listings.findBySalesChannel_IdAndProduct_Id(requireNotNull(salesChannel.id), requireNotNull(product.id))
            ?: ChannelProductListingEntity().apply {
                this.salesChannel = salesChannel
                this.product = product
            }
        listing.category = category
        listing.displayStatus = displayStatus
        listing.displayOrder = displayOrder
        listings.save(listing)
    }

    private fun SalesOfferEntity.toView() = SalesOfferView(
        requireNotNull(publicId),
        salesChannel.code,
        requireNotNull(productSku.publicId),
        salePrice,
        listPrice,
        salesStatus,
        unitsPerSale,
        organization?.publicId,
    )

    private fun ProductEntity.toView(details: Boolean = false): ProductView {
        val pid = requireNotNull(id)
        return ProductView(
            requireNotNull(publicId), requireNotNull(category.publicId), brand?.publicId, name, description, displayStatus, salesStatus, displayOrder,
            if (details) catalog.images(pid).map { ImageView(requireNotNull(it.publicId), it.storageKey, it.altText, it.displayOrder) } else emptyList(),
            if (details) {
                catalog.options(pid).map { o ->
                    OptionView(
                        requireNotNull(o.publicId),
                        o.name,
                        o.displayOrder,
                        catalog.optionValues(requireNotNull(o.id)).map {
                            OptionValueView(requireNotNull(it.publicId), it.value, it.displayOrder)
                        },
                    )
                }
            } else {
                emptyList()
            },
            if (details) {
                catalog.skus(pid).map { s ->
                    SkuView(
                        requireNotNull(s.publicId),
                        s.skuCode,
                        s.name,
                        s.salePrice,
                        s.listPrice,
                        s.salesStatus,
                        s.optionValues.mapTo(linkedSetOf()) {
                            requireNotNull(it.publicId)
                        },
                    )
                }
            } else {
                emptyList()
            },
        )
    }

    private companion object {
        const val ON_SALE = "ON_SALE"
        const val DISPLAYED = "DISPLAYED"
        const val WHOLESALE = "WHOLESALE"
        const val RETAIL = "RETAIL"
    }
}
