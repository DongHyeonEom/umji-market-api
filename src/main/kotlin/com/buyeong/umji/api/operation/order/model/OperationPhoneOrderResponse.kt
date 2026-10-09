package com.buyeong.umji.api.operation.order.model

import com.buyeong.umji.api.operation.order.dto.OperationPhoneOrderSummaryDto

data class OperationPhoneOrderResponse(
    val orders: List<OperationPhoneOrderSummaryDto>,
)