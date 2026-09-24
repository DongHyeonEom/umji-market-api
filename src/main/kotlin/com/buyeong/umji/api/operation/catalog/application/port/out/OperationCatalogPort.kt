package com.buyeong.umji.api.operation.catalog.application.port.out

import com.buyeong.umji.api.operation.catalog.application.model.BrandCommand
import com.buyeong.umji.api.operation.catalog.application.model.BrandView
import com.buyeong.umji.api.operation.catalog.application.model.CatalogResource
import com.buyeong.umji.api.operation.catalog.application.model.CategoryCommand
import com.buyeong.umji.api.operation.catalog.application.model.CategoryView
import com.buyeong.umji.api.operation.catalog.application.model.ImageCommand
import com.buyeong.umji.api.operation.catalog.application.model.OptionCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductPageView
import com.buyeong.umji.api.operation.catalog.application.model.ProductStatusCommand
import com.buyeong.umji.api.operation.catalog.application.model.ProductView
import com.buyeong.umji.api.operation.catalog.application.model.SkuCommand
import java.util.UUID

interface OperationCatalogPort {
    fun categories(): List<CategoryView>
    fun brands(page: Int, size: Int): List<BrandView>
    fun products(page: Int, size: Int): ProductPageView
    fun product(id: UUID): ProductView?
    fun createCategory(command: CategoryCommand): CatalogResource
    fun createBrand(command: BrandCommand): CatalogResource
    fun createProduct(command: ProductCommand): CatalogResource
    fun updateProduct(id: UUID, command: ProductCommand): CatalogResource?
    fun addImage(id: UUID, command: ImageCommand): CatalogResource?
    fun addOption(id: UUID, command: OptionCommand): CatalogResource?
    fun addSku(id: UUID, command: SkuCommand): CatalogResource?
    fun updateStatus(id: UUID, command: ProductStatusCommand): CatalogResource?
}