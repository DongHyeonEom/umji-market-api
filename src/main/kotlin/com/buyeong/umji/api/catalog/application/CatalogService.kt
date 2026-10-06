package com.buyeong.umji.api.catalog.application

import com.buyeong.umji.api.catalog.application.model.CategoryView
import com.buyeong.umji.api.catalog.application.model.ProductDetailView
import com.buyeong.umji.api.catalog.application.model.ProductPageView
import com.buyeong.umji.api.catalog.application.port.`in`.CatalogUseCase
import com.buyeong.umji.api.catalog.application.port.out.CatalogReadPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import java.util.UUID

class CatalogService(private val catalog: CatalogReadPort) : CatalogUseCase {
    override fun categories(channelCode: String): List<CategoryView> = catalog.categories(channel(channelCode))
    override fun products(page: Int, size: Int, channelCode: String): ProductPageView = catalog.products(page, size, channel(channelCode))
    override fun product(productId: UUID, channelCode: String): ProductDetailView =
        catalog.product(channel(channelCode), productId) ?: throw ItemNotFoundException("상품을 찾을 수 없습니다.")

    private fun channel(code: String) = code.uppercase().also {
        require(it in setOf("WHOLESALE", "RETAIL")) { "유효하지 않은 판매 채널입니다." }
    }
}
