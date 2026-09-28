package com.buyeong.umji.api.shipment.adapter

import com.buyeong.umji.api.shipment.application.ShipmentService
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ShipmentApplicationConfiguration {
    @Bean
    fun shipmentService(shipments: ShipmentStorePort, inventory: ShipmentInventoryPort) = ShipmentService(shipments, inventory)
}