package com.buyeong.umji.api.persistence.jpa.order.repository

interface ShipmentCostSummaryProjection {
    fun getPackageCount(): Long
    fun getActualCostTotal(): Long
    fun getMissingCostCount(): Long
}