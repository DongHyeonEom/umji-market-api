package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.notification.dto.NotificationEventType
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import com.buyeong.umji.api.domain.operation.audit.service.OperationAuditChangeContext
import com.buyeong.umji.api.domain.shipment.dto.ShipmentChangeDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentPackageDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentPackageItemDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentRecordDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.domain.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.domain.shipment.model.CarrierTrackingStatus
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageRequest
import com.buyeong.umji.api.domain.shipment.service.BrandShippingService
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageItemEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ShipmentService(
    private val shipments: OrderShipmentJpaEntityService,
    private val inventory: InventoryService,
    private val tracking: OfficialCarrierTrackingGateway,
    private val notifications: NotificationEventService,
    private val brandShipping: BrandShippingService,
    private val auditChanges: OperationAuditChangeContext = OperationAuditChangeContext(),
) {
    fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChangeDto {
        val carrier = carrierCode.trim()
        val tracking = trackingNumber.trim()
        val current = shipments.findForUpdate(orderId)?.toRecord() ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        require(shipments.findForUpdate(orderId)?.packages.isNullOrEmpty()) { "개별 송장 API를 사용해야 하는 주문입니다." }
        if (current.status == IN_TRANSIT && current.carrierCode == carrier && current.trackingNumber == tracking) {
            return current.toChange(false)
        }
        require(current.status == PREPARING || current.status == IN_TRANSIT) { "발송 처리 중인 주문만 송장 정보를 등록할 수 있습니다." }
        val entity = requireNotNull(shipments.findForUpdate(orderId))
        val changed = shipments.update(entity, IN_TRANSIT, carrier, tracking, operatorId).toRecord()
        auditChanges.capture(
            mapOf("shippingStatus" to current.status, "carrierCode" to current.carrierCode, "trackingNumber" to current.trackingNumber),
            mapOf("shippingStatus" to changed.status, "carrierCode" to changed.carrierCode, "trackingNumber" to changed.trackingNumber),
        )
        notifications.record(NotificationEventType.SHIPMENT_IN_TRANSIT, orderId)
        return changed.toChange(true)
    }

    fun markDelivered(orderId: UUID, operatorId: UUID): ShipmentChangeDto {
        val current = shipments.findForUpdate(orderId)?.toRecord() ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        require(shipments.findForUpdate(orderId)?.packages.isNullOrEmpty()) { "개별 송장 API를 사용해야 하는 주문입니다." }
        if (current.status == DELIVERED) return current.toChange(false)
        require(current.status == IN_TRANSIT) { "배송 중인 주문만 배송 완료 처리할 수 있습니다." }
        val entity = requireNotNull(shipments.findForUpdate(orderId))
        val delivered = shipments.update(entity, DELIVERED, current.carrierCode, current.trackingNumber, operatorId).toRecord()
        auditChanges.capture(mapOf("shippingStatus" to current.status), mapOf("shippingStatus" to delivered.status))
        notifications.record(NotificationEventType.SHIPMENT_DELIVERED, orderId)
        return delivered.toChange(true)
    }

    fun registerPackage(orderId: UUID, expectedBrandId: UUID, request: ShipmentPackageRequest, operatorId: UUID): ShipmentPackageDto {
        val shipment = shipments.findForUpdate(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        require(shipment.status == PREPARING || shipment.status == IN_TRANSIT) { "배송 준비 중인 주문만 송장을 등록할 수 있습니다." }
        val carrier = request.carrierCode.trim().uppercase()
        val trackingNumber = request.trackingNumber.trim()
        require(request.items.map { it.orderItemId }.distinct().size == request.items.size) { "송장 내 같은 품목을 중복 지정할 수 없습니다." }

        val orderItems = request.items.map { requested ->
            val item = shipment.order.items.firstOrNull { it.publicId == requested.orderItemId }
                ?: throw ItemNotFoundException("주문 품목을 찾을 수 없습니다.")
            require(shipments.allocatedBoxQuantity(requested.orderItemId) + requested.boxQuantity <= item.quantity) {
                "이미 다른 송장에 등록된 수량을 초과했습니다."
            }
            item to requested.boxQuantity
        }
        val brand = orderItems.map { it.first.sku.product.brand }.distinctBy { it?.id }.singleOrNull()
            ?: throw IllegalArgumentException("송장 하나에는 한 브랜드의 품목만 등록할 수 있습니다.")
        val brandId = brand.publicId ?: throw IllegalArgumentException("브랜드가 지정되지 않은 품목은 배송 관리자 송장에 등록할 수 없습니다.")
        require(brandId == expectedBrandId) { "송장 브랜드가 요청 경로와 일치하지 않습니다." }
        if (!brandShipping.canManageBrandOrAdmin(brandId, operatorId)) {
            throw org.springframework.security.access.AccessDeniedException("담당 브랜드 품목만 송장에 등록할 수 있습니다.")
        }

        var estimatedCost = 0L
        var completeRateSet = true
        val lineSnapshots = orderItems.map { (item, quantity) ->
            val rate = brandShipping.rate(brandId, requireNotNull(item.sku.publicId), carrier)
            val lineAmount = rate?.let { Math.multiplyExact(it.amountPerBox, quantity.toLong()) }
            if (lineAmount == null) completeRateSet = false else estimatedCost = Math.addExact(estimatedCost, lineAmount)
            Triple(item, quantity, rate?.amountPerBox to lineAmount)
        }
        val canOverrideCost = brandShipping.canConfigureBrandOrAdmin(brandId, operatorId)
        require(request.actualShippingCost == null || canOverrideCost) { "기본 운송비 기준으로만 송장을 저장할 수 있습니다." }
        require(completeRateSet || canOverrideCost) { "설정된 기본 운송비가 없는 품목은 배송 관리자가 등록해야 합니다." }
        val actualCost = request.actualShippingCost ?: estimatedCost.takeIf { completeRateSet }
            ?: throw IllegalArgumentException("기본 운송비가 없는 품목이 있어 실제 운송비 입력이 필요합니다.")
        val allocatedFreight = splitAmount(actualCost, orderItems.map { requireNotNull(it.first.id) })

        val parcel = OrderShipmentPackageEntity().apply {
            this.brand = brand
            carrierCode = carrier
            this.trackingNumber = trackingNumber
            actualShippingCost = actualCost
            status = IN_TRANSIT
        }
        parcel.items = lineSnapshots.map { (item, quantity, snapshot) ->
            OrderShipmentPackageItemEntity().apply {
                shipmentPackage = parcel
                orderItem = item
                boxQuantity = quantity
                amountPerBoxSnapshot = snapshot.first
                lineShippingCostSnapshot = allocatedFreight.getValue(requireNotNull(item.id))
            }
        }.toMutableList()
        val saved = shipments.registerPackage(shipment, parcel, operatorId)
        auditChanges.capture(
            beforeValues = mapOf("shippingStatus" to shipment.status),
            afterValues = mapOf(
                "packageId" to saved.publicId.toString(),
                "shippingStatus" to saved.status,
                "carrierCode" to saved.carrierCode,
                "trackingNumber" to saved.trackingNumber,
                "actualShippingCost" to saved.actualShippingCost,
                "brandId" to saved.brand?.publicId?.toString(),
            ),
        )
        notifications.record(NotificationEventType.SHIPMENT_IN_TRANSIT, orderId)
        return saved.toDto()
    }

    fun markPackageDelivered(orderId: UUID, packageId: UUID, operatorId: UUID): ShipmentPackageDto {
        val parcel = shipments.findPackageForUpdate(packageId) ?: throw ItemNotFoundException("송장 정보를 찾을 수 없습니다.")
        require(parcel.shipment.order.publicId == orderId) { "송장이 주문에 속하지 않습니다." }
        val brandId = parcel.brand?.publicId ?: throw ItemNotFoundException("브랜드가 지정된 송장을 찾을 수 없습니다.")
        if (!brandShipping.canManageBrandOrAdmin(brandId, operatorId)) {
            throw org.springframework.security.access.AccessDeniedException("담당 브랜드의 송장만 변경할 수 있습니다.")
        }
        val previousStatus = parcel.shipment.status
        val previousPackageStatus = parcel.status
        val saved = shipments.markPackageDelivered(parcel, operatorId)
        if (previousPackageStatus != saved.status) {
            auditChanges.capture(
                beforeValues = mapOf("packageStatus" to previousPackageStatus, "shipmentStatus" to previousStatus),
                afterValues = mapOf("packageStatus" to saved.status, "shipmentStatus" to saved.shipment.status),
            )
        }
        if (saved.shipment.status == DELIVERED && previousStatus != DELIVERED) {
            notifications.record(NotificationEventType.SHIPMENT_DELIVERED, orderId)
        }
        return saved.toDto()
    }

    fun operationPackages(operatorId: UUID): List<ShipmentPackageDto> {
        val all = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.authorities
            ?.any { it.authority == "SHIPMENT_ASSIGN" } == true
        return shipments.operationPackages(brandShipping.assignedBrandIds(operatorId), all).map { it.toDto() }
    }

    fun updateActualCost(packageId: UUID, amount: Long, operatorId: UUID, reason: String): ShipmentPackageDto {
        val parcel = shipments.findPackageForUpdate(packageId) ?: throw ItemNotFoundException("송장 정보를 찾을 수 없습니다.")
        val brandId = parcel.brand?.publicId ?: throw ItemNotFoundException("브랜드가 지정된 송장을 찾을 수 없습니다.")
        if (!brandShipping.canConfigureBrandOrAdmin(brandId, operatorId)) {
            throw org.springframework.security.access.AccessDeniedException("담당 브랜드 배송 관리자만 실제 운송비를 수정할 수 있습니다.")
        }
        val previousCost = parcel.actualShippingCost
        val freightByItemId = splitAmount(amount, parcel.items.map { requireNotNull(it.orderItem.id) })
        parcel.items.forEach { it.lineShippingCostSnapshot = freightByItemId.getValue(requireNotNull(it.orderItem.id)) }
        val updated = shipments.updateActualCost(parcel, amount, operatorId)
        auditChanges.capture(
            beforeValues = mapOf("actualShippingCost" to previousCost),
            afterValues = mapOf("actualShippingCost" to updated.actualShippingCost),
            reason = reason,
        )
        return updated.toDto()
    }

    private fun OrderShipmentPackageEntity.toDto() = ShipmentPackageDto(
        orderId = requireNotNull(shipment.order.publicId),
        packageId = requireNotNull(publicId),
        brandId = brand?.publicId,
        status = status,
        carrierCode = carrierCode,
        trackingNumber = trackingNumber,
        actualShippingCost = actualShippingCost,
        items = items.map { item ->
            ShipmentPackageItemDto(
                orderItemId = requireNotNull(item.orderItem.publicId),
                skuId = requireNotNull(item.orderItem.sku.publicId),
                skuName = item.orderItem.skuName,
                boxQuantity = item.boxQuantity,
                amountPerBox = item.amountPerBoxSnapshot,
                lineShippingCost = item.lineShippingCostSnapshot,
            )
        },
    )

    fun refreshForCustomer(customerId: UUID): Int = shipments.trackingCandidatesForCustomer(customerId).count { candidate ->
        synchronize(candidate).changed
    }

    private fun synchronize(candidate: ShipmentTrackingCandidateDto): ShipmentChangeDto {
        if (candidate.status != IN_TRANSIT) return candidate.toChange(false)
        val lookup = tracking.lookup(candidate.carrierCode, candidate.trackingNumber)
        if (lookup == CarrierTrackingStatus.DELIVERED && shipments.markDeliveredIfCurrent(candidate)) {
            notifications.record(NotificationEventType.SHIPMENT_DELIVERED, candidate.orderId)
            return candidate.toChange(true, DELIVERED)
        }
        return candidate.toChange(false)
    }

    fun prepareReadyOrders(): Int {
        var prepared = 0
        shipments.readyOrderIds().forEach { orderId ->
            if (prepareOrder(orderId).changed) prepared++
        }
        return prepared
    }

    fun prepareOrder(orderId: UUID): ShipmentChangeDto {
        val entity = shipments.findForUpdate(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        val current = entity.toRecord()
        if (current.status == PREPARING) return current.toChange(false)
        require(current.status == READY_TO_SHIP) { "배송 준비 가능한 주문 상태가 아닙니다." }
        if (entity.order.organization.paymentBeforeShipmentRequired && entity.order.payment.status != PAYMENT_CONFIRMED) {
            return current.toChange(false)
        }
        entity.order.items.forEach { item -> item.purchaseCostSnapshot = inventory.confirmCost(item.reservationKey) }
        val preparing = shipments.update(entity, PREPARING, null, null, null).toRecord()
        notifications.record(NotificationEventType.SHIPMENT_PREPARING, orderId)
        return preparing.toChange(true)
    }

    private fun OrderShipmentEntity.toRecord() = com.buyeong.umji.api.domain.shipment.dto.ShipmentRecordDto(
        orderId = requireNotNull(order.publicId),
        status = status,
        carrierCode = carrierCode,
        trackingNumber = trackingNumber,
        reservationKeys = order.items.map { it.reservationKey },
    )

    private fun com.buyeong.umji.api.domain.shipment.dto.ShipmentRecordDto.toChange(changed: Boolean) =
        ShipmentChangeDto(orderId, status, carrierCode, trackingNumber, changed)

    private fun ShipmentTrackingCandidateDto.toChange(
        changed: Boolean,
        status: String = this.status,
    ) = ShipmentChangeDto(orderId, status, carrierCode, trackingNumber, changed)

    private fun splitAmount(amount: Long, orderItemIds: List<Long>): Map<Long, Long> {
        require(orderItemIds.isNotEmpty()) { "운송비를 배분할 품목이 없습니다." }
        val base = amount / orderItemIds.size
        val remainder = amount % orderItemIds.size
        return orderItemIds.sorted().mapIndexed { index, id -> id to (base + if (index < remainder) 1 else 0) }.toMap()
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val PREPARING = "PREPARING"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
        const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"
        const val MAX_CARRIER_LENGTH = 80
        const val MAX_TRACKING_LENGTH = 100
    }
}
