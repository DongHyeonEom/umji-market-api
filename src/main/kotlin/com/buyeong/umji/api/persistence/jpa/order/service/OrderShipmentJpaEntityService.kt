package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageItemRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentRepository
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderShipmentJpaEntityService(
    private val shipments: OrderShipmentRepository,
    private val packages: OrderShipmentPackageRepository,
    private val packageItems: OrderShipmentPackageItemRepository,
    private val accounts: AccountJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
) {
    @Transactional
    fun initialize(order: PurchaseOrderEntity): OrderShipmentEntity = shipments.saveAndFlush(
        OrderShipmentEntity().apply {
            this.order = order
            status = READY_TO_SHIP
            createdAt = order.orderedAt
            updatedAt = order.orderedAt
        },
    )

    @Transactional
    fun findForUpdate(orderId: UUID): OrderShipmentEntity? = shipments.findForUpdateByOrderId(orderId)

    fun readyOrderIds(): List<UUID> = shipments.findReadyOrderIds()

    fun trackingCandidatesForCustomer(customerId: UUID): List<ShipmentTrackingCandidateDto> {
        val organizationId = requireNotNull(organizations.activeBuyerForAccountPublicId(customerId)?.id) {
            "계정의 활성 구매자 그룹을 찾을 수 없습니다."
        }
        return packages.findTrackingCandidatesForOrganization(organizationId)
    }

    @Transactional
    fun markDeliveredIfCurrent(candidate: ShipmentTrackingCandidateDto): Boolean {
        val current = packages.findForUpdateByPublicId(candidate.packageId) ?: return false
        if (current.status != IN_TRANSIT || current.carrierCode != candidate.carrierCode || current.trackingNumber != candidate.trackingNumber) {
            return false
        }
        current.status = DELIVERED
        current.updatedAt = Instant.now()
        packages.saveAndFlush(current)
        updateAggregate(current.shipment)
        return true
    }

    @Transactional
    fun registerPackage(shipment: OrderShipmentEntity, parcel: OrderShipmentPackageEntity, operatorId: UUID): OrderShipmentPackageEntity {
        shipment.packages.add(parcel)
        parcel.shipment = shipment
        parcel.processedBy = accounts.findByPublicId(operatorId) ?: throw ItemNotFoundException("처리자 계정을 찾을 수 없습니다.")
        val saved = packages.saveAndFlush(parcel)
        val taxInvoice = shipment.order.taxInvoice
        if (taxInvoice?.status == WAITING_FOR_SHIPMENT) {
            val orderDate = shipment.order.orderedAt.atZone(KST).toLocalDate()
            taxInvoice.writtenDate = orderDate
            taxInvoice.supplyDate = orderDate
            taxInvoice.status = READY_FOR_ISSUANCE
        }
        updateAggregate(shipment)
        return saved
    }

    @Transactional
    fun findPackageForUpdate(packageId: UUID): OrderShipmentPackageEntity? = packages.findForUpdateByPublicId(packageId)

    fun allocatedBoxQuantity(orderItemId: UUID): Long = packageItems.allocatedBoxQuantity(orderItemId)

    fun operationPackages(brandIds: Set<UUID>, includeAll: Boolean): List<OrderShipmentPackageEntity> =
        packages.findAllForOperation(brandIds.ifEmpty { setOf(UUID(0, 0)) }, includeAll)

    @Transactional
    fun updateActualCost(parcel: OrderShipmentPackageEntity, amount: Long, operatorId: UUID): OrderShipmentPackageEntity {
        require(amount >= 0) { "실제 운송비는 0원 이상이어야 합니다." }
        parcel.actualShippingCost = amount
        parcel.processedBy = accounts.findByPublicId(operatorId) ?: throw ItemNotFoundException("처리자 계정을 찾을 수 없습니다.")
        parcel.updatedAt = Instant.now()
        return packages.saveAndFlush(parcel)
    }

    @Transactional
    fun markPackageDelivered(parcel: OrderShipmentPackageEntity, operatorId: UUID): OrderShipmentPackageEntity {
        if (parcel.status == DELIVERED) return parcel
        require(parcel.status == IN_TRANSIT) { "배송 중인 송장만 배송 완료 처리할 수 있습니다." }
        parcel.status = DELIVERED
        parcel.processedBy = accounts.findByPublicId(operatorId) ?: throw ItemNotFoundException("처리자 계정을 찾을 수 없습니다.")
        parcel.updatedAt = Instant.now()
        val saved = packages.saveAndFlush(parcel)
        updateAggregate(parcel.shipment)
        return saved
    }

    private fun updateAggregate(shipment: OrderShipmentEntity) {
        val packageRows = packages.findAllByShipment_Id(requireNotNull(shipment.id))
        if (packageRows.isEmpty()) return
        val orderItems = shipment.order.items
        val allocated = packageRows.flatMap { it.items }.groupBy { it.orderItem.id }
            .mapValues { (_, rows) -> rows.sumOf { it.boxQuantity } }
        val allBoxesAllocated = orderItems.isNotEmpty() && orderItems.all { item ->
            (allocated[item.id] ?: 0) >= item.quantity
        }
        shipment.status = if (allBoxesAllocated && packageRows.all { it.status == DELIVERED }) DELIVERED else IN_TRANSIT
        shipment.updatedAt = Instant.now()
        shipments.saveAndFlush(shipment)
    }

    @Transactional
    fun update(
        shipment: OrderShipmentEntity,
        status: String,
        carrierCode: String?,
        trackingNumber: String?,
        operatorId: UUID?,
    ): OrderShipmentEntity {
        shipment.status = status
        shipment.carrierCode = carrierCode
        shipment.trackingNumber = trackingNumber
        shipment.processedBy = operatorId?.let { accounts.findByPublicId(it) ?: error("처리자 계정을 찾을 수 없습니다.") }
        shipment.updatedAt = Instant.now()
        val taxInvoice = shipment.order.taxInvoice
        if (status == IN_TRANSIT && taxInvoice?.status == WAITING_FOR_SHIPMENT) {
            val orderDate = shipment.order.orderedAt.atZone(KST).toLocalDate()
            taxInvoice.writtenDate = orderDate
            taxInvoice.supplyDate = orderDate
            taxInvoice.status = READY_FOR_ISSUANCE
        }
        return shipments.saveAndFlush(shipment)
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
        const val WAITING_FOR_SHIPMENT = "WAITING_FOR_SHIPMENT"
        const val READY_FOR_ISSUANCE = "READY_FOR_ISSUANCE"
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
