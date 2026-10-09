package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class ChannelListingCommandDto(val channelCode: String, val productId: UUID, val categoryId: UUID, val displayStatus: String, val displayOrder: Int)