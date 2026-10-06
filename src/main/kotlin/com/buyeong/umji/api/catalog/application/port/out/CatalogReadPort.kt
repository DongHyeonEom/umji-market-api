package com.buyeong.umji.api.catalog.application.port.out

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import java.util.UUID

interface CatalogReadPort {
    fun categories(channelCode: String): List<CategoryView>
    fun products(page: Int, size: Int, channelCode: String): ProductPageView
    fun product(channelCode: String, productId: UUID): ProductDetailView?
}
