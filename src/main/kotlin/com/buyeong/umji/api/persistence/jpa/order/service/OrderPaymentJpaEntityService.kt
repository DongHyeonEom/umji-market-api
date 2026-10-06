package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderStatusHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderPaymentHistoryRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderPaymentRepository
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderPaymentJpaEntityService(
    private val payments: OrderPaymentRepository,
    private val histories: OrderPaymentHistoryRepository,
    private val accounts: AccountJpaEntityService,
    private val orders: OrderJpaEntityService,
) {
    @Transactional
    fun initialize(order: PurchaseOrderEntity): OrderPaymentEntity {
        val payment = payments.saveAndFlush(
            OrderPaymentEntity().apply {
                this.order = order
                paymentMethod = BANK_TRANSFER
                status = WAITING_FOR_DEPOSIT
                updatedAt = order.orderedAt
            },
        )
        histories.save(
            OrderPaymentHistoryEntity().apply {
                this.payment = payment
                toStatus = WAITING_FOR_DEPOSIT
                changedAt = order.orderedAt
            },
        )
        return payment
    }

    fun findForUpdate(orderId: UUID): OrderPaymentEntity? = payments.findForUpdateByOrderId(orderId)

    fun findAllForOperation(statuses: Set<String>, pageable: Pageable): Page<OrderPaymentEntity> =
        payments.findAllForOperation(statuses, pageable)

    fun findAllByStatus(status: String, pageable: Pageable): Page<OrderPaymentEntity> = payments.findAllByStatus(status, pageable)

    @Transactional
    fun saveChange(payment: OrderPaymentEntity, status: String, processedBy: UUID): OrderPaymentEntity {
        val actor = accounts.findByPublicId(processedBy) ?: error("처리자 계정을 찾을 수 없습니다.")
        val now = Instant.now()
        val fromStatus = payment.status
        payment.status = status
        payment.updatedAt = now
        payments.saveAndFlush(payment)
        histories.save(
            OrderPaymentHistoryEntity().apply {
                this.payment = payment
                this.fromStatus = fromStatus
                toStatus = status
                this.processedBy = actor
                changedAt = now
            },
        )
        if (status == PAYMENT_CONFIRMED) {
            val order = payment.order
            check(order.status == ORDER_PENDING_PAYMENT) { "결제 대기 중인 주문만 결제 완료할 수 있습니다." }
            order.status = ORDER_PAID
            orders.saveAndFlush(order)
            orders.saveHistory(
                OrderStatusHistoryEntity().apply {
                    this.order = order
                    this.fromStatus = ORDER_PENDING_PAYMENT
                    toStatus = ORDER_PAID
                    reasonCode = PAYMENT_CONFIRMED
                    changedAt = now
                },
            )
        }
        return payment
    }

    private companion object {
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val WAITING_FOR_DEPOSIT = "WAITING_FOR_DEPOSIT"
        const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"
        const val ORDER_PENDING_PAYMENT = "PENDING_PAYMENT"
        const val ORDER_PAID = "PAID"
    }
}
