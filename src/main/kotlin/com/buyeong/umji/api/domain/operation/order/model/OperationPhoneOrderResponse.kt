package com.buyeong.umji.api.domain.operation.order.model

import com.buyeong.umji.api.domain.operation.order.dto.OperationPhoneOrderSummaryDto

data class OperationPhoneOrderResponse(
    val orders: List<OperationPhoneOrderSummaryDto>,
)