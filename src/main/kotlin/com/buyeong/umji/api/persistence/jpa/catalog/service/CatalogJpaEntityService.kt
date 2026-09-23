package com.buyeong.umji.api.persistence.jpa.catalog

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
) {
    fun displayedCategories(status: String): List<CategoryEntity> = categories.findAllByDisplayStatusAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(status)
    fun categories(): List<CategoryEntity> = categories.findAllByDeletedAtIsNullOrderByDisplayOrderAscNameAsc()
    fun category(id: UUID): CategoryEntity? = categories.findByPublicIdAndDeletedAtIsNull(id)
    fun brands(pageable: Pageable): Page<BrandEntity> = brands.findAllByDeletedAtIsNull(pageable)
    fun brand(id: UUID): BrandEntity? = brands.findByPublicIdAndDeletedAtIsNull(id)
    fun existsBrandName(name: String): Boolean = brands.existsByName(name)
    fun publicProducts(displayStatus: String, salesStatus: String, pageable: Pageable): Page<ProductEntity> = products.findAllByDisplayStatusAndSalesStatusAndDeletedAtIsNull(displayStatus, salesStatus, pageable)
    fun publicProduct(id: UUID, displayStatus: String, salesStatus: String): ProductEntity? = products.findByPublicIdAndDisplayStatusAndSalesStatusAndDeletedAtIsNull(id, displayStatus, salesStatus)
    fun products(pageable: Pageable): Page<ProductEntity> = products.findAllByDeletedAtIsNull(pageable)
    fun product(id: UUID): ProductEntity? = products.findByPublicIdAndDeletedAtIsNull(id)
    fun sku(id: UUID): ProductSkuEntity? = skus.findByPublicId(id)
    fun skus(productId: Long, salesStatus: String): List<ProductSkuEntity> = skus.findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId, salesStatus)
    fun skus(productId: Long): List<ProductSkuEntity> = skus.findAllByProductIdOrderBySalePriceAsc(productId)
    fun existsSkuCode(code: String): Boolean = skus.existsBySkuCode(code)
    fun images(productId: Long): List<ProductImageEntity> = images.findAllByProductIdOrderByDisplayOrderAscIdAsc(productId)
    fun options(productId: Long): List<ProductOptionEntity> = options.findAllByProductIdOrderByDisplayOrderAscIdAsc(productId)
    fun optionValues(optionId: Long): List<ProductOptionValueEntity> = optionValues.findAllByOptionIdOrderByDisplayOrderAscIdAsc(optionId)
    fun optionValues(ids: Collection<UUID>): List<ProductOptionValueEntity> = optionValues.findAllByPublicIdIn(ids)
    @Transactional fun save(category: CategoryEntity): CategoryEntity = categories.save(category)
    @Transactional fun save(brand: BrandEntity): BrandEntity = brands.save(brand)
    @Transactional fun save(product: ProductEntity): ProductEntity = products.save(product)
    @Transactional fun save(sku: ProductSkuEntity): ProductSkuEntity = skus.save(sku)
    @Transactional fun save(image: ProductImageEntity): ProductImageEntity = images.save(image)
    @Transactional fun save(option: ProductOptionEntity): ProductOptionEntity = options.save(option)
    @Transactional fun save(value: ProductOptionValueEntity): ProductOptionValueEntity = optionValues.save(value)
}
