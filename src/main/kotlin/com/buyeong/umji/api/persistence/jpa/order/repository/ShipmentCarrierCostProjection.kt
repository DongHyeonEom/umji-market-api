package com.buyeong.umji.api.persistence.jpa.order.repository

interface ShipmentCarrierCostProjection {
    fun getCarrierCode(): String
    fun getPackageCount(): Long
    fun getActualCostTotal(): Long
    fun getMissingCostCount(): Long
}