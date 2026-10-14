package com.buyeong.umji.api.domain.operation.profit.model

import java.util.UUID

data class ItemProfitResponse(val from: String, val until: String, val items: List<ItemProfitRowResponse>)

data class ItemProfitRowResponse(
    val skuId: UUID,
    val skuCode: String,
    val skuName: String,
    val brandId: UUID?,
    val brandName: String?,
    val orderItemCount: Long,
    val completeCount: Long,
    val incompleteCostCount: Long,
    val incompleteShippingCount: Long,
    val salesAmount: Long,
    val purchaseCost: Long,
    val shippingCost: Long,
    val netProfit: Long,
)
