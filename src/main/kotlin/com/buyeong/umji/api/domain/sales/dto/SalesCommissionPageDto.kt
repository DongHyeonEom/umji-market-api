package com.buyeong.umji.api.domain.sales.dto

data class SalesCommissionPageDto(
    val items: List<SalesCommissionViewDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)