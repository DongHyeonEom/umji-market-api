package com.buyeong.umji.api.domain.operation.catalog.service

import com.buyeong.umji.api.domain.operation.catalog.dto.BrandCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.CatalogResourceDto
import com.buyeong.umji.api.domain.operation.catalog.dto.CategoryCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ChannelCategoryCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ChannelListingCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ImageCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.OptionCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.ProductStatusCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferViewDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SkuPricingCommandDto
import com.buyeong.umji.api.domain.operation.catalog.dto.SkuCommandDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.catalog.service.OperationCatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class OperationCatalogService(private val catalog: OperationCatalogJpaEntityService) {
    @Transactional(readOnly = true)
    fun categories(channelCode: String) = catalog.categories(channel(channelCode))

    @Transactional(readOnly = true)
    fun brands(page: Int, size: Int) = catalog.brands(page, size)

    @Transactional(readOnly = true)
    fun products(page: Int, size: Int) = catalog.products(page, size)

    @Transactional(readOnly = true)
    fun product(id: UUID) = catalog.product(id) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
    fun createCategory(
        command: CategoryCommandDto,
    ): CatalogResourceDto {
        return catalog.createCategory(command.copy(name = command.name.trim()))
    }
    fun createChannelCategory(command: ChannelCategoryCommandDto): CatalogResourceDto {
        channel(command.channelCode)
        return catalog.createChannelCategory(command.copy(channelCode = channel(command.channelCode), name = command.name.trim()))
    }
    fun updateChannelListing(command: ChannelListingCommandDto): CatalogResourceDto {
        channel(command.channelCode)
        return catalog.updateChannelListing(command.copy(channelCode = channel(command.channelCode))) ?: missing()
    }
    fun updateSalesOffer(command: SalesOfferCommandDto): com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferViewDto {
        val channelCode = channel(command.channelCode)
        return catalog.updateSalesOffer(command.copy(channelCode = channelCode)) ?: missing()
    }
    fun updateSellerSalesOffer(organizationId: UUID, command: SalesOfferCommandDto): com.buyeong.umji.api.domain.operation.catalog.dto.SalesOfferViewDto {
        val channelCode = channel(command.channelCode)
        return catalog.updateSellerSalesOffer(organizationId, command.copy(channelCode = channelCode)) ?: missing()
    }
    fun updateSkuPricing(command: SkuPricingCommandDto) =
        catalog.updateSkuPricing(command) ?: missing()
    fun updateSellerSkuPricing(organizationId: UUID, command: SkuPricingCommandDto) =
        catalog.updateSellerSkuPricing(organizationId, command) ?: missing()
    fun createBrand(
        command: BrandCommandDto,
    ): CatalogResourceDto {
        return catalog.createBrand(command.copy(name = command.name.trim()))
    }
    fun updateSellerChannelListing(organizationId: UUID, command: ChannelListingCommandDto): CatalogResourceDto {
        channel(command.channelCode)
        return catalog.updateSellerChannelListing(organizationId, command.copy(channelCode = channel(command.channelCode))) ?: missing()
    }
    fun sellerBrands(organizationId: UUID, page: Int, size: Int) = catalog.sellerBrands(organizationId, page, size)
    fun createSellerBrand(organizationId: UUID, command: BrandCommandDto): CatalogResourceDto {
        return catalog.createSellerBrand(organizationId, command.copy(name = command.name.trim()))
    }
    fun sellerProducts(organizationId: UUID, page: Int, size: Int) = catalog.sellerProducts(organizationId, page, size)
    fun sellerProduct(organizationId: UUID, id: UUID) = catalog.sellerProduct(organizationId, id) ?: missing()
    fun createSellerProduct(organizationId: UUID, command: ProductCommandDto): CatalogResourceDto {
        return catalog.createSellerProduct(organizationId, command.normalized())
    }
    fun updateSellerProduct(organizationId: UUID, id: UUID, command: ProductCommandDto): CatalogResourceDto {
        return catalog.updateSellerProduct(organizationId, id, command.normalized()) ?: missing()
    }
    fun createProduct(command: ProductCommandDto): CatalogResourceDto {
        return catalog.createProduct(command.normalized())
    }
    fun updateProduct(id: UUID, command: ProductCommandDto): CatalogResourceDto {
        return catalog.updateProduct(id, command.normalized()) ?: missing()
    }
    fun addImage(
        id: UUID,
        command: ImageCommandDto,
    ) = catalog.addImage(id, command.copy(storageKey = command.storageKey.trim(), altText = command.altText.clean())) ?: missing()
    fun addOption(id: UUID, command: OptionCommandDto) =
        catalog.addOption(id, command.copy(name = command.name.trim(), values = command.values.map { it.copy(value = it.value.trim()) })) ?: missing()
    fun addSku(id: UUID, command: SkuCommandDto): CatalogResourceDto {
        return catalog.addSku(id, command.normalized()) ?: missing()
    }
    fun updateStatus(
        id: UUID,
        command: ProductStatusCommandDto,
    ): CatalogResourceDto {
        return catalog.updateStatus(id, command)
            ?: missing()
    }
    private fun ProductCommandDto.normalized() = copy(
        name = name.trim(),
        description = description.clean(),
        images = images.map {
            it.copy(storageKey = it.storageKey.trim(), altText = it.altText.clean())
        },
        options = options.map { it.copy(name = it.name.trim(), values = it.values.map { v -> v.copy(value = v.value.trim()) }) },
        skus = skus.map { it.normalized() },
    )
    private fun SkuCommandDto.normalized() = copy(skuCode = skuCode.trim(), name = name.trim())
    private fun String?.clean() = this?.trim()?.ifBlank { null }
    private fun channel(code: String): String = code.uppercase()
    private fun missing(): Nothing = throw ItemNotFoundException("상품 또는 관련 리소스를 찾을 수 없습니다.")
}
