package com.buyeong.umji.api.domain.operation.order.dto

import java.time.Instant

data class OperationOrderQueryDto(
    val from: Instant,
    val until: Instant,
    val organizationName: String?,
    val ordererName: String?,
    val paymentStatus: String?,
    val shipmentStatus: String?,
    val orderNumber: String?,
    val page: Int,
    val size: Int,
)