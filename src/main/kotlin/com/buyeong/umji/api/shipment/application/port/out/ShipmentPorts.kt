package com.buyeong.umji.api.shipment.application.port.out

interface ShipmentTrackingPort {
    fun lookup(carrierCode: String, trackingNumber: String): com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
}
