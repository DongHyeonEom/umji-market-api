package com.buyeong.umji.api.domain.operation.order.controller

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderQueryDto
import com.buyeong.umji.api.domain.operation.order.model.OperationOrderDetailResponse
import com.buyeong.umji.api.domain.operation.order.model.OperationOrderPageResponse
import com.buyeong.umji.api.domain.operation.order.model.toResponse
import com.buyeong.umji.api.domain.operation.order.service.OperationOrderService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/api/operation/orders")
@Validated
class OperationOrderController(
    private val orders: OperationOrderService,
) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_READ')")
    fun search(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) until: LocalDate,
        @RequestParam(required = false) @Size(max = 100) organizationName: String?,
        @RequestParam(required = false) @Size(max = 100) ordererName: String?,
        @RequestParam(required = false) @Size(max = 40) paymentStatus: String?,
        @RequestParam(required = false) @Size(max = 40) shipmentStatus: String?,
        @RequestParam(required = false) @Size(max = 100) orderNumber: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationOrderPageResponse {
        require(!until.isBefore(from)) { "조회 종료일은 시작일보다 빠를 수 없습니다." }
        val fromAt = from.atStartOfDay(KST).toInstant()
        val untilAt = runCatching { until.plusDays(1) }
            .getOrElse { throw IllegalArgumentException("조회 종료일이 지원 범위를 벗어났습니다.") }
            .atStartOfDay(KST)
            .toInstant()
        val result = orders.search(
            OperationOrderQueryDto(fromAt, untilAt, organizationName, ordererName, paymentStatus, shipmentStatus, orderNumber, page, size),
        )
        return OperationOrderPageResponse(result.items.map { it.toResponse() }, result.page, result.size, result.totalElements, result.totalPages)
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_READ')")
    fun detail(@PathVariable orderId: UUID): OperationOrderDetailResponse = orders.detail(orderId).toResponse()

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}