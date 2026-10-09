package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class ImageViewDto(val id: UUID, val storageKey: String, val altText: String?, val displayOrder: Int)