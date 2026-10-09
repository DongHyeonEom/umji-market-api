package com.buyeong.umji.api.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.order.model.ShippingHolidayRequest
import com.buyeong.umji.api.order.model.ShippingHolidayResponse
import com.buyeong.umji.api.order.service.ShippingHolidayService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/operation/shipping-holidays")
@Tag(name = "배송 휴무일 관리", description = "배송 준비 자동 전환에 반영할 휴무일 등록·조회·삭제 API")
class OperationShippingHolidayController(private val currentAccounts: CurrentAccountService, private val holidays: ShippingHolidayService) {
    @Operation(summary = "배송 휴무일 목록 조회", description = "등록된 배송 휴무일과 설명 목록을 반환")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun list() = holidays.list().map { ShippingHolidayResponse(it.date, it.description) }

    @Operation(summary = "배송 휴무일 등록", description = "날짜와 선택 설명을 검증해 배송 휴무일로 등록")
    @PostMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun register(@Valid @RequestBody request: ShippingHolidayRequest): ShippingHolidayResponse {
        val description = request.description?.trim()?.ifBlank { null }
        holidays.register(request.date, description, currentAccounts.activeAccountPublicId())
        return ShippingHolidayResponse(request.date, description)
    }

    @Operation(summary = "배송 휴무일 삭제", description = "지정한 날짜의 배송 휴무일을 삭제")
    @DeleteMapping("/{date}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun remove(@Parameter(description = "배송 휴무 날짜(YYYY-MM-DD)") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate) = holidays.remove(date)
}