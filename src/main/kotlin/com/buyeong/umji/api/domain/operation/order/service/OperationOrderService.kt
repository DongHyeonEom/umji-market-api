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
        require(query.from < query.until) { "조회 시작일은 종료일보다 이전이어야 합니다." }
        require(query.page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(query.size in 1..100) { "페이지 크기는 1~100이어야 합니다." }
        require(query.paymentStatus == null || query.paymentStatus in PAYMENT_STATUSES) { "유효하지 않은 결제 상태입니다." }
        require(query.shipmentStatus == null || query.shipmentStatus in SHIPMENT_STATUSES) { "유효하지 않은 배송 상태입니다." }
        return orders.search(
            query.copy(
                organizationName = query.organizationName?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
                ordererName = query.ordererName?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
                orderNumber = query.orderNumber?.trim()?.lowercase()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    fun detail(orderId: UUID): OperationOrderDetailDto = orders.detail(orderId)

    private companion object {
        val PAYMENT_STATUSES = setOf(
            "WAITING_FOR_DEPOSIT",
            "PARTIAL_PAYMENT_REVIEW_REQUIRED",
            "PAYMENT_ISSUE_REVIEW_REQUIRED",
            "PAYMENT_CONFIRMED",
            "REFUND_PENDING",
            "REFUNDED",
        )
        val SHIPMENT_STATUSES = setOf("READY_TO_SHIP", "PREPARING", "IN_TRANSIT", "DELIVERED")
    }
}