package com.buyeong.umji.api.shipment.adapter.out.tracking

import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus

interface OfficialCarrierTrackingAdapter {
    fun supports(carrierCode: String): Boolean

    fun lookup(trackingNumber: String): CarrierTrackingStatus
}