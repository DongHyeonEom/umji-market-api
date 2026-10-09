package com.buyeong.umji.api.domain.operation.catalog.dto

data class OptionCommandDto(val name: String, val displayOrder: Int, val values: List<OptionValueCommandDto>)