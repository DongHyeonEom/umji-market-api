package com.buyeong.umji.api.domain.catalog.dto

import com.buyeong.umji.api.domain.catalog.dto.CategoryViewDto
import java.util.UUID

data class CategoryViewDto(val id: UUID, val name: String, val path: String, val depth: Int, val channelCode: String = "WHOLESALE")