package com.buyeong.umji.api.operation.payment.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.payment.model.OperationPaymentPageResponse
import com.buyeong.umji.api.payment.model.PaymentStatusRequest
import com.buyeong.umji.api.payment.model.PaymentStatusResponse
import com.buyeong.umji.api.payment.model.toResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/payments")
@Validated
class OperationPaymentController(
    private val currentAccounts: CurrentAccountPort,
    private val payments: TransactionalPaymentUseCase,
) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun queue(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationPaymentPageResponse = payments.queue(status, page, size).toResponse()

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun updateStatus(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: PaymentStatusRequest,
    ): PaymentStatusResponse = payments.updateStatus(orderId, request.status, currentAccounts.activeAccountPublicId()).toResponse()
}
