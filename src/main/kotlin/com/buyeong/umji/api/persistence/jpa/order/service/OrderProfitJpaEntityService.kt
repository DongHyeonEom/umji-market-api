package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderItemProfitRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class OrderProfitJpaEntityService(
    private val items: OrderItemProfitRepository,
    private val packages: OrderShipmentPackageRepository,
) {
    fun orderItems(from: Instant, until: Instant): List<OrderItemEntity> = items.findForProfit(from, until)
    fun packages(from: Instant, until: Instant): List<OrderShipmentPackageEntity> =
        packages.findAllByShipment_Order_OrderedAtGreaterThanEqualAndShipment_Order_OrderedAtLessThan(from, until)
}
