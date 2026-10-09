package com.buyeong.umji.api.operation.catalog.dto

data class ProductPageViewDto(val items: List<ProductViewDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)