package com.buyeong.umji.api.catalog.application.port.out

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import java.util.UUID

interface CatalogReadPort {
    fun categories(): List<CategoryView>
    fun products(page: Int, size: Int): ProductPageView
    fun product(productId: UUID): ProductDetailView?
}