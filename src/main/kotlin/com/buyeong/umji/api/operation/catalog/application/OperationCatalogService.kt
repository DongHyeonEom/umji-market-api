package com.buyeong.umji.api.operation.catalog.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.catalog.application.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.application.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.application.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.application.model.ChannelCategoryCommand
import com.buyeong.umji.api.operation.catalog.application.model.ChannelListingCommand
import com.buyeong.umji.api.operation.catalog.application.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.application.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.application.model.SkuCommand
import com.buyeong.umji.api.operation.catalog.application.model.SalesOfferCommand
import com.buyeong.umji.api.operation.catalog.application.port.`in`.OperationCatalogUseCase
import com.buyeong.umji.api.operation.catalog.application.port.out.OperationCatalogPort
import java.util.UUID

class OperationCatalogService(private val catalog: OperationCatalogPort) : OperationCatalogUseCase {
    override fun categories(channelCode: String) = catalog.categories(channel(channelCode))
    override fun brands(page: Int, size: Int) = catalog.brands(page, size)
    override fun products(page: Int, size: Int) = catalog.products(page, size)
    override fun product(id: UUID) = catalog.product(id) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
    override fun createCategory(
        command: CategoryCommand,
    ): CatalogResource {
        validateDisplay(command.displayStatus)
        return catalog.createCategory(command.copy(name = command.name.trim()))
    }
    override fun createChannelCategory(command: ChannelCategoryCommand): CatalogResource {
        channel(command.channelCode)
        validateDisplay(command.displayStatus)
        return catalog.createChannelCategory(command.copy(channelCode = channel(command.channelCode), name = command.name.trim()))
    }
    override fun updateChannelListing(command: ChannelListingCommand): CatalogResource {
        channel(command.channelCode)
        validateDisplay(command.displayStatus)
        return catalog.updateChannelListing(command.copy(channelCode = channel(command.channelCode))) ?: missing()
    }
    override fun updateSalesOffer(command: SalesOfferCommand): com.buyeong.umji.api.operation.catalog.application.model.SalesOfferView {
        val channelCode = channel(command.channelCode)
        require(command.salePrice >= 0) { "판매 가격은 0원 이상이어야 합니다." }
        require(command.listPrice == null || command.listPrice >= 0) { "정가는 0원 이상이어야 합니다." }
        validateSales(command.salesStatus)
        return catalog.updateSalesOffer(command.copy(channelCode = channelCode)) ?: missing()
    }
    override fun createBrand(
        command: BrandCommand,
    ): CatalogResource {
        validateDisplay(command.displayStatus)
        return catalog.createBrand(command.copy(name = command.name.trim()))
    }
    override fun createProduct(command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.createProduct(command.normalized())
    }
    override fun updateProduct(id: UUID, command: ProductCommand): CatalogResource {
        validateProduct(command)
        return catalog.updateProduct(id, command.normalized()) ?: missing()
    }
    override fun addImage(
        id: UUID,
        command: ImageCommand,
    ) = catalog.addImage(id, command.copy(storageKey = command.storageKey.trim(), altText = command.altText.clean())) ?: missing()
    override fun addOption(id: UUID, command: OptionCommand) =
        catalog.addOption(id, command.copy(name = command.name.trim(), values = command.values.map { it.copy(value = it.value.trim()) })) ?: missing()
    override fun addSku(id: UUID, command: SkuCommand): CatalogResource {
        validateSales(command.salesStatus)
        return catalog.addSku(id, command.normalized()) ?: missing()
    }
    override fun updateStatus(
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
