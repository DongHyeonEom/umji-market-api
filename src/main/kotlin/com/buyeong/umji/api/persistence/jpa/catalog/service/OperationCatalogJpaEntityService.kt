package com.buyeong.umji.api.persistence.jpa.catalog.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.domain.operation.catalog.dto.BrandCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.BrandViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.CatalogResourceDto
import com.buyeong.umji.api.domain.operation.catalog.dto.CategoryCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.CategoryViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ChannelCategoryCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ChannelListingCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ImageCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ImageViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.OptionCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.OptionValueViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.OptionViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductPageViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductStatusCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SkuCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SkuViewDto
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
        CategoryViewDto(requireNotNull(it.publicId), it.parent?.publicId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus)
    }

    @Transactional(readOnly = true)
    fun brands(
        page: Int,
        size: Int,
    ) = catalog.brands(PageRequest.of(page, size, Sort.by("name"))).content.map { BrandViewDto(requireNotNull(it.publicId), it.name, it.displayStatus) }

    @Transactional(readOnly = true)
    fun products(
        page: Int,
        size: Int,
    ): ProductPageViewDto {
        val p = catalog.products(PageRequest.of(page, size, Sort.by("id").descending()))
        return ProductPageViewDto(p.content.map { it.toView() }, p.number, p.size, p.totalElements, p.totalPages)
    }

    @Transactional(readOnly = true)
    fun product(id: UUID) = catalog.product(id)?.toView(true)

    fun createCategory(command: CategoryCommandDto): CatalogResourceDto {
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
        return CatalogResourceDto(requireNotNull(catalog.save(e).publicId))
    }

    fun createChannelCategory(command: ChannelCategoryCommandDto): CatalogResourceDto {
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
        return CatalogResourceDto(requireNotNull(catalog.save(entity).publicId))
    }

    fun updateChannelListing(command: ChannelListingCommandDto): CatalogResourceDto? {
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
        return CatalogResourceDto(requireNotNull(listings.save(listing).publicId))
    }

    fun updateSellerChannelListing(organizationPublicId: UUID, command: ChannelListingCommandDto): CatalogResourceDto? {
        val channel = channel(command.channelCode)
        val product = catalog.sellerProduct(command.productId, organizationPublicId) ?: return null
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
        return CatalogResourceDto(requireNotNull(listings.save(listing).publicId))
    }

    fun updateSalesOffer(command: SalesOfferCommandDto): SalesOfferViewDto? {
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

    fun updateSellerSalesOffer(organizationPublicId: UUID, command: SalesOfferCommandDto): SalesOfferViewDto? {
        require(command.unitsPerSale == null || command.unitsPerSale > 0) { "판매 단위 입수 수량은 1 이상이어야 합니다." }
        val organization = organizations.findByPublicId(organizationPublicId) ?: return null
        val channel = channel(command.channelCode)
        val sku = catalog.sku(command.skuId) ?: return null
        require(sku.product.organization?.id == organization.id) { "판매 Organization 소유 상품의 SKU만 오퍼로 등록할 수 있습니다." }
        require(sku.salesStatus == ON_SALE && sku.product.salesStatus == ON_SALE && sku.product.displayStatus == DISPLAYED) {
            "노출·판매 가능한 상품의 SKU만 오퍼로 등록할 수 있습니다."
        }
        val offer = offers.findBySalesChannel_IdAndProductSku_IdAndOrganization_Id(
            requireNotNull(channel.id),
            requireNotNull(sku.id),
            requireNotNull(organization.id),
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
        command: BrandCommandDto,
    ): CatalogResourceDto {
        require(!catalog.existsBrandName(command.name)) { "이미 존재하는 브랜드입니다." }
        return CatalogResourceDto(
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
    fun sellerBrands(organizationPublicId: UUID, page: Int, size: Int) =
        catalog.sellerBrands(organizationPublicId, PageRequest.of(page, size, Sort.by("name"))).content.map {
            BrandViewDto(requireNotNull(it.publicId), it.name, it.displayStatus)
        }

    fun createSellerBrand(organizationPublicId: UUID, command: BrandCommandDto): CatalogResourceDto {
        val organization = organizations.findByPublicId(organizationPublicId) ?: throw ItemNotFoundException("Organization을 찾을 수 없습니다.")
        require(!catalog.sellerBrandExists(organizationPublicId, command.name)) { "이미 등록된 브랜드입니다." }
        val brand = BrandEntity().apply {
            this.organization = organization
            name = command.name
            displayStatus = command.displayStatus
        }
        return CatalogResourceDto(requireNotNull(catalog.save(brand).publicId))
    }

    fun sellerProducts(organizationPublicId: UUID, page: Int, size: Int): ProductPageViewDto {
        val p = catalog.sellerProducts(organizationPublicId, PageRequest.of(page, size, Sort.by("id").descending()))
        return ProductPageViewDto(p.content.map { it.toView() }, p.number, p.size, p.totalElements, p.totalPages)
    }

    fun sellerProduct(organizationPublicId: UUID, id: UUID) = catalog.sellerProduct(id, organizationPublicId)?.toView(true)

    fun createSellerProduct(organizationPublicId: UUID, command: ProductCommandDto): CatalogResourceDto {
        val organization = organizations.findByPublicId(organizationPublicId) ?: throw ItemNotFoundException("Organization을 찾을 수 없습니다.")
        val product = buildProduct(command, organization)
        val saved = catalog.save(product)
        wholesaleListing(saved, saved.category, saved.displayStatus, saved.displayOrder)
        command.images.forEach { catalog.save(image(saved, it)) }
        command.options.forEach { saveOption(saved, it) }
        command.skus.forEach { catalog.save(sku(saved, it)) }
        return CatalogResourceDto(requireNotNull(saved.publicId))
    }

    fun updateSellerProduct(organizationPublicId: UUID, id: UUID, command: ProductCommandDto): CatalogResourceDto? {
        val organization = organizations.findByPublicId(organizationPublicId) ?: return null
        val product = catalog.sellerProduct(id, organizationPublicId) ?: return null
        applyProduct(product, command, organization)
        wholesaleListing(product, product.category, product.displayStatus, product.displayOrder)
        return CatalogResourceDto(requireNotNull(product.publicId))
    }
    fun createProduct(
        command: ProductCommandDto,
    ): CatalogResourceDto {
        val product = buildProduct(command)
        val saved = catalog.save(product)
        wholesaleListing(saved, saved.category, saved.displayStatus, saved.displayOrder)
        command.images.forEach { catalog.save(image(saved, it)) }
        command.options.forEach { saveOption(saved, it) }
        command.skus.forEach {
            catalog.save(sku(saved, it))
        }
        return CatalogResourceDto(requireNotNull(saved.publicId))
    }
    fun updateProduct(id: UUID, command: ProductCommandDto): CatalogResourceDto? {
        val p =
            catalog.product(id) ?: return null
        if (p.organization != null) return null
        applyProduct(p, command, null)
        wholesaleListing(p, p.category, p.displayStatus, p.displayOrder)
        return CatalogResourceDto(requireNotNull(p.publicId))
    }
    fun addImage(id: UUID, command: ImageCommandDto): CatalogResourceDto? {
        val p =
            catalog.product(id) ?: return null
        if (p.organization != null) return null
        return CatalogResourceDto(requireNotNull(catalog.save(image(p, command)).publicId))
    }
    fun addOption(id: UUID, command: OptionCommandDto): CatalogResourceDto? {
        val p =
            catalog.product(id) ?: return null
        if (p.organization != null) return null
        return CatalogResourceDto(requireNotNull(saveOption(p, command).publicId))
    }
    fun addSku(id: UUID, command: SkuCommandDto): CatalogResourceDto? {
        val p =
            catalog.product(id) ?: return null
        if (p.organization != null) return null
        val savedSku = catalog.save(sku(p, command))
        return CatalogResourceDto(requireNotNull(savedSku.publicId))
    }
    fun updateStatus(id: UUID, command: ProductStatusCommandDto): CatalogResourceDto? {
        val p =
            catalog.product(id) ?: return null
        if (p.organization != null) return null
        p.displayStatus = command.displayStatus
        p.salesStatus = command.salesStatus
        wholesaleListing(p, p.category, command.displayStatus, p.displayOrder)
        offers.findAllBySalesChannel_IdAndProductSku_Product_Id(requireNotNull(channel(WHOLESALE).id), requireNotNull(p.id))
            .forEach { it.salesStatus = command.salesStatus }
        return CatalogResourceDto(requireNotNull(p.publicId))
    }

    private fun buildProduct(c: ProductCommandDto, organization: com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity? = null) =
        ProductEntity().also { applyProduct(it, c, organization) }
    private fun applyProduct(
        p: ProductEntity,
        c: ProductCommandDto,
        organization: com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity?,
    ) {
        p.category = category(c.categoryId).also {
            require(it.salesChannel.code == WHOLESALE) { "상품 기본 카테고리는 WHOLESALE 채널이어야 합니다." }
        }
        p.organization = organization
        p.brand = c.brandId?.let(::brand)?.also {
            require(it.organization?.id == organization?.id) { "상품과 같은 Organization 소유의 브랜드를 선택해야 합니다." }
        }
        p.name = c.name
        p.description = c.description
        p.displayStatus =
            c.displayStatus
        p.salesStatus = c.salesStatus
        p.displayOrder = c.displayOrder
    }
    private fun image(p: ProductEntity, c: ImageCommandDto) = ProductImageEntity().apply {
        product = p
        storageKey = c.storageKey
        altText = c.altText
        displayOrder = c.displayOrder
    }
    private fun saveOption(
        p: ProductEntity,
        c: OptionCommandDto,
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
        c: SkuCommandDto,
    ): ProductSkuEntity {
        require(!catalog.existsSkuCode(requireNotNull(p.id), c.skuCode)) { "같은 상품에 이미 등록된 SKU 코드입니다." }
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

    private fun SalesOfferEntity.toView() = SalesOfferViewDto(
        requireNotNull(publicId),
        salesChannel.code,
        requireNotNull(productSku.publicId),
        salePrice,
        listPrice,
        salesStatus,
        unitsPerSale,
        organization?.publicId,
    )

    private fun ProductEntity.toView(details: Boolean = false): ProductViewDto {
        val pid = requireNotNull(id)
        return ProductViewDto(
            requireNotNull(publicId), requireNotNull(category.publicId), brand?.publicId, name, description, displayStatus, salesStatus, displayOrder,
            if (details) catalog.images(pid).map { ImageViewDto(requireNotNull(it.publicId), it.storageKey, it.altText, it.displayOrder) } else emptyList(),
            if (details) {
                catalog.options(pid).map { o ->
                    OptionViewDto(
                        requireNotNull(o.publicId),
                        o.name,
                        o.displayOrder,
                        catalog.optionValues(requireNotNull(o.id)).map {
                            OptionValueViewDto(requireNotNull(it.publicId), it.value, it.displayOrder)
                        },
                    )
                }
            } else {
                emptyList()
            },
            if (details) {
                catalog.skus(pid).map { s ->
                    SkuViewDto(
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