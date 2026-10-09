package com.buyeong.umji.api.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.order.dto.CancellationChangeDto
import com.buyeong.umji.api.order.model.CancellationChangeResponse
import com.buyeong.umji.api.order.model.CancellationQueueItemResponse
import com.buyeong.umji.api.order.model.CancellationQueueResponse
import com.buyeong.umji.api.order.model.CancellationResolutionRequest
import com.buyeong.umji.api.order.service.OrderCancellationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Validated
@Tag(name = "주문 취소", description = "구매자 취소 요청과 운영자 취소 요청 처리 API")
class OrderCancellationController(
    private val currentAccounts: CurrentAccountService,
    private val cancellations: OrderCancellationService,
) {
    @Operation(summary = "주문 취소 요청", description = "주문 소유자의 취소 가능 여부를 확인하고 취소 요청을 등록")
    @PostMapping("/api/orders/{orderId}/cancellation")
    @ResponseStatus(HttpStatus.CREATED)
    fun request(@Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID) = cancellations.request(currentAccounts.activeAccountPublicId(), orderId).toResponse()

    @Operation(summary = "취소 요청 대기 목록 조회", description = "처리 대기 중인 주문 취소 요청을 페이지 단위로 반환")
    @GetMapping("/api/operation/order-cancellations")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun queue(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) =
        cancellations.queue(page, size).let {
            CancellationQueueResponse(
                it.items.map { item ->
                    CancellationQueueItemResponse(item.orderId, item.orderNumber, item.accountId, item.requestedAt)
                },
                it.page,
                it.size,
                it.totalElements,
                it.totalPages,
            )
        }

    @Operation(summary = "주문 취소 요청 처리", description = "운영자의 승인 여부를 반영해 주문 취소 요청을 처리")
    @PatchMapping("/api/operation/order-cancellations/{orderId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun resolve(@Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID, @Valid @RequestBody request: CancellationResolutionRequest) =
        cancellations.resolve(orderId, request.approved, currentAccounts.activeAccountPublicId()).toResponse()

    private fun com.buyeong.umji.api.order.dto.CancellationChangeDto.toResponse() =
        CancellationChangeResponse(orderId, orderStatus, requestStatus)
}