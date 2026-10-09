package com.buyeong.umji.api.domain.inventory.dto

data class MovementPageStateDto(val items: List<MovementStateDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)