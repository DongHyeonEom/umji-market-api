package com.buyeong.umji.api.domain.order.dto

data class CancellationQueuePageDto(val items: List<CancellationQueueItemDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)