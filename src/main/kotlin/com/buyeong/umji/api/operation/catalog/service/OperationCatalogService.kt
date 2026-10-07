package com.buyeong.umji.api.operation.catalog.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.catalog.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.model.ChannelCategoryCommand
import com.buyeong.umji.api.operation.catalog.model.ChannelListingCommand
import com.buyeong.umji.api.operation.catalog.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.model.SkuCommand
import com.buyeong.umji.api.operation.catalog.model.SalesOfferCommand
import com.buyeong.umji.api.persistence.jpa.catalog.service.OperationCatalogJpaEntityService
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        command: CategoryCommand,
    ): CatalogResource {
        validateDisplay(command.displayStatus)
        return catalog.createCategory(command.copy(name = command.name.trim()))
    }
    fun createChannelCategory(command: ChannelCategoryCommand): CatalogResource {
        channel(command.channelCode)
        validateDisplay(command.displayStatus)
        return catalog.createChannelCategory(command.copy(channelCode = channel(command.channelCode), name = command.name.trim()))
    }
    fun updateChannelListing(command: ChannelListingCommand): CatalogResource {
        channel(command.channelCode)
        validateDisplay(command.displayStatus)
        return catalog.updateChannelListing(command.copy(channelCode = channel(command.channelCode))) ?: missing()
    }
    fun updateSalesOffer(command: SalesOfferCommand): com.buyeong.umji.api.operation.catalog.model.SalesOfferView {
        val channelCode = channel(command.channelCode)
        require(command.salePrice >= 0) { "판매 가격은 0원 이상이어야 합니다." }
        require(command.listPrice == null || command.listPrice >= 0) { "정가는 0원 이상이어야 합니다." }
        require(command.unitsPerSale == null || command.unitsPerSale > 0) { "판매 단위 입수 수량은 1 이상이어야 합니다." }
        require(channelCode != "RETAIL" || command.unitsPerSale == null || command.unitsPerSale == 1) { "RETAIL 오퍼의 판매 단위 입수 수량은 1이어야 합니다." }
        validateSales(command.salesStatus)
        return catalog.updateSalesOffer(command.copy(channelCode = channelCode)) ?: missing()
    }
    fun updateSellerSalesOffer(organizationId: UUID, command: SalesOfferCommand): com.buyeong.umji.api.operation.catalog.model.SalesOfferView {
        val channelCode = channel(command.channelCode)
        require(command.salePrice >= 0) { "판매 가격은 0원 이상이어야 합니다." }
        require(command.listPrice == null || command.listPrice >= 0) { "정가는 0원 이상이어야 합니다." }
        require(command.unitsPerSale == null || command.unitsPerSale > 0) { "판매 단위 입수 수량은 1 이상이어야 합니다." }
        require(channelCode != "RETAIL" || command.unitsPerSale == null || command.unitsPerSale == 1) { "RETAIL 오퍼의 판매 단위 입수 수량은 1이어야 합니다." }
        validateSales(command.salesStatus)
        return catalog.updateSellerSalesOffer(organizationId, command.copy(channelCode = channelCode)) ?: missing()
    }
    fun createBrand(
        command: BrandCommand,
    ): CatalogResource {
        validateDisplay(command.displayStatus)
        return catalog.createBrand(command.copy(name = command.name.trim()))
    }
    fun updateSellerChannelListing(organizationId: UUID, command: ChannelListingCommand): CatalogResource {
        channel(command.channelCode)
        validateDisplay(command.displayStatus)
        return catalog.updateSellerChannelListing(organizationId, command.copy(channelCode = channel(command.channelCode))) ?: missing()
    }
    fun sellerBrands(organizationId: UUID, page: Int, size: Int) = catalog.sellerBrands(organizationId, page, size)
    fun createSellerBrand(organizationId: UUID, command: BrandCommand): CatalogResource {
        validateDisplay(command.displayStatus)
        require(command.name.isNotBlank() && command.name.length <= 100) { "브랜드명은 1자 이상 100자 이하여야 합니다." }
        return catalog.createSellerBrand(organizationId, command.copy(name = command.name.trim()))
    }
    fun sellerProducts(organizationId: UUID, page: Int, size: Int) = catalog.sellerProducts(organizationId, page, size)
    fun sellerProduct(organizationId: UUID, id: UUID) = catalog.sellerProduct(organizationId, id) ?: missing()
    fun createSellerProduct(organizationId: UUID, command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.createSellerProduct(organizationId, command.normalized())
    }
    fun updateSellerProduct(organizationId: UUID, id: UUID, command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.updateSellerProduct(organizationId, id, command.normalized()) ?: missing()
    }
    fun createProduct(command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.createProduct(command.normalized())
    }
    fun updateProduct(id: UUID, command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.updateProduct(id, command.normalized()) ?: missing()
    }
    fun addImage(
        id: UUID,
        command: ImageCommand,
    ) = catalog.addImage(id, command.copy(storageKey = command.storageKey.trim(), altText = command.altText.clean())) ?: missing()
    fun addOption(id: UUID, command: OptionCommand) =
        catalog.addOption(id, command.copy(name = command.name.trim(), values = command.values.map { it.copy(value = it.value.trim()) })) ?: missing()
    fun addSku(id: UUID, command: SkuCommand): CatalogResource {
        validateSales(command.salesStatus)
        return catalog.addSku(id, command.normalized()) ?: missing()
    }
    fun updateStatus(
        id: UUID,
        command: ProductStatusCommand,
    ): CatalogResource {
        validateDisplay(command.displayStatus)
        validateSales(command.salesStatus)
        return catalog.updateStatus(id, command)
            ?: missing()
    }
    private fun validateProduct(c: ProductCommand) {
        validateDisplay(c.displayStatus)
        validateSales(c.salesStatus)
        c.skus.forEach { validateSales(it.salesStatus) }
    }
    private fun ProductCommand.normalized() = copy(
        name = name.trim(),
        description = description.clean(),
        images = images.map {
            it.copy(storageKey = it.storageKey.trim(), altText = it.altText.clean())
        },
        options = options.map { it.copy(name = it.name.trim(), values = it.values.map { v -> v.copy(value = v.value.trim()) }) },
        skus = skus.map { it.normalized() },
    )
    private fun SkuCommand.normalized() = copy(skuCode = skuCode.trim(), name = name.trim())
    private fun String?.clean() = this?.trim()?.ifBlank { null }
    private fun validateDisplay(s: String) = require(s in setOf("DISPLAYED", "HIDDEN")) { "유효하지 않은 상품 노출 상태입니다." }
    private fun validateSales(s: String) = require(s in setOf("ON_SALE", "STOPPED")) { "유효하지 않은 판매 상태입니다." }
    private fun channel(code: String): String = code.uppercase().also {
        require(it in setOf("WHOLESALE", "RETAIL")) { "유효하지 않은 판매 채널입니다." }
    }
    private fun missing(): Nothing = throw ItemNotFoundException("상품 또는 관련 리소스를 찾을 수 없습니다.")
}
