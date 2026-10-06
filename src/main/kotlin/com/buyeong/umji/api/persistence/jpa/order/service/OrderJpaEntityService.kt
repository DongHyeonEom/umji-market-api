package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderNumberSequenceEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderStatusHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderNumberSequenceRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderStatusHistoryRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderJpaEntityService(
    private val orders: PurchaseOrderRepository,
    private val histories: OrderStatusHistoryRepository,
    private val sequences: OrderNumberSequenceRepository,
) {
    fun findWithItems(id: UUID, buyerGroupId: Long): PurchaseOrderEntity? = orders.findWithItemsByPublicIdAndBuyerGroupId(id, buyerGroupId)
    fun findAll(buyerGroupId: Long, pageable: Pageable): Page<PurchaseOrderEntity> = orders.findAllByBuyerGroup_Id(buyerGroupId, pageable)

    @Transactional fun saveAndFlush(order: PurchaseOrderEntity): PurchaseOrderEntity = orders.saveAndFlush(order)

    @Transactional fun saveHistory(history: OrderStatusHistoryEntity): OrderStatusHistoryEntity = histories.save(history)

    @Transactional fun lockedSequence(date: LocalDate): OrderNumberSequenceEntity? = sequences.findLockedByOrderDate(date)

    @Transactional fun saveAndFlushSequence(sequence: OrderNumberSequenceEntity): OrderNumberSequenceEntity = sequences.saveAndFlush(sequence)
}
