package com.buyeong.umji.api.domain.order.dto

import java.util.UUID

data class AdminPhoneOrderLineDto(val salesOfferId: UUID, val quantity: Int)