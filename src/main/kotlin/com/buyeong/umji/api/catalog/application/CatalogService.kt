package com.buyeong.umji.api.catalog.application

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import com.buyeong.umji.api.catalog.application.port.`in`.CatalogUseCase
import com.buyeong.umji.api.catalog.application.port.out.CatalogReadPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import java.util.UUID

class CatalogService(private val catalog: CatalogReadPort) : CatalogUseCase {
    override fun categories(): List<CategoryView> = catalog.categories()
    override fun products(page: Int, size: Int): ProductPageView = catalog.products(page, size)
    override fun product(productId: UUID): ProductDetailView =
        catalog.product(productId) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")
}
