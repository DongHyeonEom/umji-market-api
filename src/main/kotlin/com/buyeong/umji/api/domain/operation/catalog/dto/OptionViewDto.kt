package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class OptionViewDto(val id: UUID, val name: String, val displayOrder: Int, val values: List<OptionValueViewDto>)