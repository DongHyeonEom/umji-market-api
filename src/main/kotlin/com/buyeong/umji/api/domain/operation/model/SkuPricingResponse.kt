package com.buyeong.umji.api.domain.operation.model

import com.buyeong.umji.api.domain.operation.catalog.dto.SkuPricingViewDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "SKU 가격 정책 설정 결과")
data class SkuPricingResponse(
    val skuId: UUID,
    val productId: UUID,
    val retailSalePrice: Long,
    val wholesaleSalePrice: Long,
    val wholesaleMinimumQuantity: Int,
    val salesStatus: String,
    val unitsPerSale: Int,
    val salesOfferId: UUID,
    val organizationId: UUID?,
) {
    companion object {
        fun from(view: SkuPricingViewDto) = SkuPricingResponse(
            view.skuId, view.productId, view.retailSalePrice, view.wholesaleSalePrice,
            view.wholesaleMinimumQuantity, view.salesStatus, view.unitsPerSale,
            view.salesOfferId, view.organizationId,
        )
    }
}
