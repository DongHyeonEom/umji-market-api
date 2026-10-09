package com.buyeong.umji.api.operation.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.operation.order.model.OperationManualTaxInvoiceRequest
import com.buyeong.umji.api.operation.order.model.OperationTaxInvoicePageResponse
import com.buyeong.umji.api.operation.order.model.OperationTaxInvoiceResponse
import com.buyeong.umji.api.operation.order.service.OperationTaxInvoiceService
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/orders/tax-invoices")
@Validated
@Tag(name = "운영 세금계산서", description = "홈택스 수기 발행 결과 조회와 등록 API")
class OperationTaxInvoiceController(
    private val currentAccounts: CurrentAccountService,
    private val invoices: OperationTaxInvoiceService,
) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    @Operation(summary = "수기 발행 대상 및 완료 목록", description = "발행 준비 또는 수기 발행 완료 상태의 주문 조회")
    fun queue(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OperationTaxInvoicePageResponse = OperationTaxInvoicePageResponse.from(invoices.queue(page, size))

    @PostMapping("/{orderId}/manual-issue")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    @Operation(summary = "홈택스 수기 발행 결과 등록", description = "승인번호와 세금계산서 금액·일자를 기록하고 발행 완료로 고정")
    fun recordManualIssue(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: OperationManualTaxInvoiceRequest,
    ): OperationTaxInvoiceResponse = OperationTaxInvoiceResponse.from(
        invoices.recordManualIssue(
            orderId, currentAccounts.activeAccountPublicId(), request.approvalNumber, request.issuedAt,
            request.writtenDate, request.supplyDate, request.supplyAmount, request.taxAmount,
            request.totalAmount, request.reason,
        ),
    )
}
