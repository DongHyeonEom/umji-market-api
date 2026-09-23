package com.buyeong.umji.api.catalog.application.port.`in`

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import java.util.UUID

interface CatalogUseCase {
    fun categories(): List<CategoryView>
    fun products(page: Int, size: Int): ProductPageView
    fun product(productId: UUID): ProductDetailView
}
