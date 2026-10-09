package com.buyeong.umji.api.domain.order.dto

import java.util.UUID

data class CancellationChangeDto(val orderId: UUID, val orderStatus: String, val requestStatus: String)