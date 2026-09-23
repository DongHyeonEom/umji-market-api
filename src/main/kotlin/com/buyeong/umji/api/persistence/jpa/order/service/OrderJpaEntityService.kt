package com.buyeong.umji.api.persistence.jpa.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderJpaEntityService(
    private val orders: PurchaseOrderRepository,
    private val histories: OrderStatusHistoryRepository,
    private val sequences: OrderNumberSequenceRepository,
) {
    fun findWithItems(id: UUID, accountId: Long): PurchaseOrderEntity? = orders.findWithItemsByPublicIdAndAccountId(id, accountId)
    fun findAll(accountId: Long, pageable: Pageable): Page<PurchaseOrderEntity> = orders.findAllByAccountId(accountId, pageable)
    @Transactional fun saveAndFlush(order: PurchaseOrderEntity): PurchaseOrderEntity = orders.saveAndFlush(order)
    @Transactional fun saveHistory(history: OrderStatusHistoryEntity): OrderStatusHistoryEntity = histories.save(history)
    @Transactional fun lockedSequence(date: LocalDate): OrderNumberSequenceEntity? = sequences.findLockedByOrderDate(date)
    @Transactional fun saveAndFlushSequence(sequence: OrderNumberSequenceEntity): OrderNumberSequenceEntity = sequences.saveAndFlush(sequence)
}
