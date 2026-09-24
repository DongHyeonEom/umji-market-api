package com.buyeong.umji.api.operation.catalog.adapter.out.persistence

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.catalog.application.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.application.model.BrandView
import com.buyeong.umji.api.operation.catalog.application.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.application.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.application.model.CategoryView
import com.buyeong.umji.api.operation.catalog.application.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.application.model.ImageView
import com.buyeong.umji.api.operation.catalog.application.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.application.model.OptionValueView
import com.buyeong.umji.api.operation.catalog.application.model.OptionView
import com.buyeong.umji.api.operation.catalog.application.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductPageView
import com.buyeong.umji.api.operation.catalog.application.model.ProductView
import com.buyeong.umji.api.operation.catalog.application.model.SkuView
import com.buyeong.umji.api.operation.catalog.application.port.out.OperationCatalogPort
import com.buyeong.umji.api.persistence.jpa.catalog.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductImageEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductOptionEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductOptionValueEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional
class JpaOperationCatalogAdapter(private val catalog: CatalogJpaEntityService) : OperationCatalogPort {
    @Transactional(readOnly = true)
    override fun categories() = catalog.categories().map {
        CategoryView(requireNotNull(it.publicId), it.parent?.publicId, it.name, it.path, it.depth, it.displayOrder, it.displayStatus)
    }

    @Transactional(readOnly = true)
    override fun brands(
        page: Int,
        size: Int,
    ) = catalog.brands(PageRequest.of(page, size, Sort.by("name"))).content.map { BrandView(requireNotNull(it.publicId), it.name, it.displayStatus) }

    @Transactional(readOnly = true)
    override fun products(
        page: Int,
        size: Int,
    ): ProductPageView {
        val p = catalog.products(PageRequest.of(page, size, Sort.by("id").descending()))
        return ProductPageView(p.content.map { it.toView() }, p.number, p.size, p.totalElements, p.totalPages)
    }

    @Transactional(readOnly = true)
    override fun product(id: UUID) = catalog.product(id)?.toView(true)

    override fun createCategory(command: CategoryCommand): CatalogResource {
        val parent = command.parentId?.let(::category)
        val e = CategoryEntity().apply {
            name = command.name
            this.parent =
                parent
            depth = (parent?.depth ?: -1) + 1
            path = parent?.path?.let { "$it/$name" } ?: name
            displayOrder = command.displayOrder
            displayStatus = command.displayStatus
        }
        return CatalogResource(requireNotNull(catalog.save(e).publicId))
    }
    override fun createBrand(
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
    override fun createProduct(
        command: ProductCommand,
    ): CatalogResource {
        val product = buildProduct(command)
        val saved = catalog.save(product)
        command.images.forEach { catalog.save(image(saved, it)) }
        command.options.forEach { saveOption(saved, it) }
        command.skus.forEach { catalog.save(sku(saved, it)) }
        return CatalogResource(requireNotNull(saved.publicId))
    }
    override fun updateProduct(id: UUID, command: ProductCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        applyProduct(p, command)
        return CatalogResource(requireNotNull(p.publicId))
    }
    override fun addImage(id: UUID, command: ImageCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        return CatalogResource(requireNotNull(catalog.save(image(p, command)).publicId))
    }
    override fun addOption(id: UUID, command: OptionCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        return CatalogResource(requireNotNull(saveOption(p, command).publicId))
    }
    override fun addSku(id: UUID, command: SkuCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        return CatalogResource(requireNotNull(catalog.save(sku(p, command)).publicId))
    }
    override fun updateStatus(id: UUID, command: ProductStatusCommand): CatalogResource? {
        val p =
            catalog.product(id) ?: return null
        p.displayStatus = command.displayStatus
        p.salesStatus = command.salesStatus
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
}