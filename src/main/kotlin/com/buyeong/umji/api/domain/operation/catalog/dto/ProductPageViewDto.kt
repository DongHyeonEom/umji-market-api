package com.buyeong.umji.api.domain.operation.catalog.dto

data class ProductPageViewDto(val items: List<ProductViewDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)