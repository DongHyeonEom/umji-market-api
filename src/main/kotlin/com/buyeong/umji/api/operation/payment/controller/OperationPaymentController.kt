package com.buyeong.umji.api.operation.payment.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.payment.model.OperationPaymentPageResponse
import com.buyeong.umji.api.payment.model.PaymentStatusRequest
import com.buyeong.umji.api.payment.model.PaymentStatusResponse
import com.buyeong.umji.api.payment.model.toResponse
import com.buyeong.umji.api.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/payments")
@Validated
@Tag(name = "운영 결제 관리", description = "주문 입금 상태 조회와 결제 상태 변경 API")
class OperationPaymentController(
    private val currentAccounts: CurrentAccountService,
    private val payments: PaymentService,
) {
    @Operation(summary = "결제 대기 목록 조회", description = "결제 대기 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun queue(
        @Parameter(description = "조회할 상태 코드") @RequestParam(required = false) status: String?,
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationPaymentPageResponse = payments.queue(status, page, size).toResponse()

    @Operation(summary = "주문수정", description = "주문 입금 상태 조회와 결제 상태 변경 API. /{orderId}/status 경로에서 주문수정를 수행")
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun updateStatus(
        @Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID,
        @Valid @RequestBody request: PaymentStatusRequest,
    ): PaymentStatusResponse = payments.updateStatus(orderId, request.status, currentAccounts.activeAccountPublicId()).toResponse()
}
