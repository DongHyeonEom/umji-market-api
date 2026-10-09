package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "배송 휴무일 등록 요청")
data class ShippingHolidayRequest(
    @field:Schema(description = "배송 휴무 날짜", example = "2026-10-04", format = "date", type = "string", required = true)
    val date: LocalDate,

    @field:Size(max = 200,)
    @field:Schema(description = "선택적 휴무일 설명", example = "추석 연휴", type = "string", required = false)
    val description: String? = null,
)