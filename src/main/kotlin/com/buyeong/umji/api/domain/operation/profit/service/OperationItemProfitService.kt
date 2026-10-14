package com.buyeong.umji.api.domain.operation.profit.service

import com.buyeong.umji.api.domain.operation.profit.model.ItemProfitResponse
import com.buyeong.umji.api.domain.operation.profit.model.ItemProfitRowResponse
import com.buyeong.umji.api.persistence.jpa.order.service.OrderProfitJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class OperationItemProfitService(
    private val orderProfit: OrderProfitJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun query(from: LocalDate, until: LocalDate, skuId: UUID?, brandId: UUID?): ItemProfitResponse {
        require(!until.isBefore(from)) { "조회 종료일은 시작일보다 빠를 수 없습니다." }
        val start = from.atStartOfDay(KST).toInstant()
        val end = until.plusDays(1).atStartOfDay(KST).toInstant()
        val items = orderProfit.orderItems(start, end).filter { item ->
            (skuId == null || item.sku.publicId == skuId) && (brandId == null || item.sku.product.brand?.publicId == brandId)
        }
        val itemsById = items.associateBy { requireNotNull(it.id) }
        val freightByItem = mutableMapOf<Long, Long>()
        val missingFreight = mutableSetOf<Long>()
        val shippedBoxesByItem = mutableMapOf<Long, Int>()
        orderProfit.packages(start, end).forEach { parcel ->
            parcel.items.forEach lineLoop@{ line ->
                val id = line.orderItem.id ?: return@lineLoop
                if (id !in itemsById) return@lineLoop
                shippedBoxesByItem[id] = Math.addExact(shippedBoxesByItem[id] ?: 0, line.boxQuantity)
                if (parcel.actualShippingCost == null || line.lineShippingCostSnapshot == null) missingFreight += id
                else freightByItem[id] = Math.addExact(freightByItem[id] ?: 0L, line.lineShippingCostSnapshot!!)
            }
        }
        val rows = items.groupBy { it.sku.id }.values.map { skuItems ->
            val sku = skuItems.first().sku
            var completeCount = 0L
            var missingCost = 0L
            var missingShip = 0L
            var sales = 0L
            var cost = 0L
            var shipping = 0L
            skuItems.forEach { item ->
                val id = requireNotNull(item.id)
                val hasCost = item.purchaseCostSnapshot != null
                val hasShipping = (shippedBoxesByItem[id] ?: 0) >= item.quantity && id !in missingFreight
                if (!hasCost) missingCost++
                if (!hasShipping) missingShip++
                if (hasCost && hasShipping) {
                    completeCount++
                    sales = Math.addExact(sales, item.lineAmount)
                    cost = Math.addExact(cost, requireNotNull(item.purchaseCostSnapshot))
                    shipping = Math.addExact(shipping, freightByItem[id] ?: 0L)
                }
            }
            val product = sku.product
            ItemProfitRowResponse(
                requireNotNull(sku.publicId), sku.skuCode, sku.name, product.brand?.publicId, product.brand?.name,
                skuItems.size.toLong(), completeCount, missingCost, missingShip, sales, cost, shipping, sales - cost - shipping,
            )
        }.sortedBy { it.skuCode }
        return ItemProfitResponse(from.toString(), until.toString(), rows)
    }

    private companion object { val KST: ZoneId = ZoneId.of("Asia/Seoul") }
}
