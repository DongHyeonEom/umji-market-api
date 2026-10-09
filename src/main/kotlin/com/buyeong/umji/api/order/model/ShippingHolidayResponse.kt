package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "배송 휴무일 응답")
data class ShippingHolidayResponse(
    @field:Schema(description = "배송 휴무 날짜", example = "2026-10-04", format = "date", type = "string", required = true)
    val date: LocalDate,

    @field:Schema(description = "휴무일 설명", example = "추석 연휴", type = "string", required = true)
    val description: String?,
)