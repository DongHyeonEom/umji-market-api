package com.buyeong.umji.api.operation.catalog.dto

import java.util.UUID

data class ChannelCategoryCommandDto(val channelCode: String, val name: String, val parentId: UUID?, val displayOrder: Int, val displayStatus: String)