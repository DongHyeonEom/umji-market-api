package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderDetailDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderDetailItemDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderPageDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderQueryDto
import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderShipmentDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OperationOrderJpaEntityService(
    private val orders: PurchaseOrderRepository,
    private val packages: OrderShipmentPackageRepository,
) {
    fun search(query: OperationOrderQueryDto): OperationOrderPageDto {
        val result = orders.searchForOperation(
            query.from,
            query.until,
            query.organizationName,
            query.ordererName,
            query.paymentStatus,
            query.shipmentStatus,
            query.orderNumber,
            PageRequest.of(query.page, query.size, Sort.unsorted()),
        )
        return OperationOrderPageDto(result.content, result.number, result.size, result.totalElements, result.totalPages)
    }

    fun detail(orderId: UUID): OperationOrderDetailDto {
        val order = orders.findByPublicId(orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")
        val shipmentPackages = packages.findAllByShipment_Order_PublicId(orderId)
        return OperationOrderDetailDto(
            orderId = requireNotNull(order.publicId),
            orderNumber = order.orderNumber,
            orderStatus = order.status,
            orderSource = order.orderSource,
            orderedAt = order.orderedAt,
            organizationId = requireNotNull(order.organization.publicId),
            organizationName = order.organization.displayName,
            ordererName = order.account.name,
            ordererPhone = order.account.phone,
            subtotalAmount = order.subtotalAmount,
            totalAmount = order.totalAmount,
            paymentMethod = order.payment.paymentMethod,
            paymentStatus = order.payment.status,
            shipmentStatus = order.shipment.status,
            taxInvoiceRequested = order.taxInvoice != null,
            taxInvoiceStatus = order.taxInvoice?.status,
            items = order.items.map { item ->
                OperationOrderDetailItemDto(item.productName, item.skuName, item.skuCode, item.unitPrice, item.quantity, item.lineAmount)
            },
            shipments = shipmentPackages.map { parcel ->
                OperationOrderShipmentDto(
                    packageId = requireNotNull(parcel.publicId),
                    brandId = parcel.brand?.publicId,
                    status = parcel.status,
                    carrierCode = parcel.carrierCode,
                    trackingNumber = parcel.trackingNumber,
                    actualShippingCost = parcel.actualShippingCost,
                )
            },
        )
    }
}