package com.buyeong.umji.api.persistence.jpa.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementCarrierDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementItemDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementPackageDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementQueryDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.catalog.repository.BrandRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentPackageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ShipmentSettlementJpaEntityService(
    private val brands: BrandRepository,
    private val packages: OrderShipmentPackageRepository,
) {
    fun monthly(query: ShipmentSettlementQueryDto): ShipmentSettlementDto {
        val brand = brands.findByPublicIdAndDeletedAtIsNull(query.brandId)
            ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")
        val summary = packages.aggregateShippingCosts(query.brandId, query.from, query.until)
        val carriers = packages.aggregateShippingCostsByCarrier(query.brandId, query.from, query.until).map {
            ShipmentSettlementCarrierDto(it.getCarrierCode(), it.getPackageCount(), it.getActualCostTotal(), it.getMissingCostCount())
        }
        val result = packages.findAllForSettlement(
            query.brandId,
            query.from,
            query.until,
            PageRequest.of(query.page, query.size, Sort.unsorted()),
        )
        val details = result.content.map { parcel ->
            val order = parcel.shipment.order
            ShipmentSettlementPackageDto(
                packageId = requireNotNull(parcel.publicId),
                orderId = requireNotNull(order.publicId),
                orderNumber = order.orderNumber,
                orderedAt = order.orderedAt,
                registeredAt = parcel.createdAt,
                shippingStatus = parcel.status,
                carrierCode = parcel.carrierCode,
                trackingNumber = parcel.trackingNumber,
                actualShippingCost = parcel.actualShippingCost,
                items = parcel.items.map { item ->
                    ShipmentSettlementItemDto(
                        orderItemId = requireNotNull(item.orderItem.publicId),
                        skuName = item.orderItem.skuName,
                        boxQuantity = item.boxQuantity,
                        amountPerBox = item.amountPerBoxSnapshot,
                        lineShippingCost = item.lineShippingCostSnapshot,
                    )
                },
            )
        }
        return ShipmentSettlementDto(
            brandId = query.brandId,
            brandName = brand.name,
            month = query.month,
            packageCount = summary.getPackageCount(),
            actualCostTotal = summary.getActualCostTotal(),
            pricedPackageCount = summary.getPackageCount() - summary.getMissingCostCount(),
            missingCostCount = summary.getMissingCostCount(),
            carriers = carriers,
            items = details,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}