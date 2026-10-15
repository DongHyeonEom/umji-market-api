package com.buyeong.umji.api.domain.operation.order.service

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderDetailDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderPageDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderQueryDto
import com.buyeong.umji.api.persistence.jpa.order.service.OperationOrderJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OperationOrderService(private val orders: OperationOrderJpaEntityService) {
    fun search(query: OperationOrderQueryDto): OperationOrderPageDto {
        return orders.search(
            query.copy(
                organizationName = query.organizationName?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
                ordererName = query.ordererName?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
                orderNumber = query.orderNumber?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    fun detail(orderId: UUID): OperationOrderDetailDto = orders.detail(orderId)

}
