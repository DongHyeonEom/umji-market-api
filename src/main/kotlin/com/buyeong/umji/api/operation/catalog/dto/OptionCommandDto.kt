package com.buyeong.umji.api.operation.catalog.dto

data class OptionCommandDto(val name: String, val displayOrder: Int, val values: List<OptionValueCommandDto>)