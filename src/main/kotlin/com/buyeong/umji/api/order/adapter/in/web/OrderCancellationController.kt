package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
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
import java.time.Instant
import java.util.UUID

@RestController
@Validated
class OrderCancellationController(
    private val currentAccounts: CurrentAccountPort,
    private val cancellations: TransactionalOrderCancellationUseCase,
) {
    @PostMapping("/api/orders/{orderId}/cancellation")
    @ResponseStatus(HttpStatus.CREATED)
    fun request(@PathVariable orderId: UUID) = cancellations.request(currentAccounts.activeAccountPublicId(), orderId).toResponse()

    @GetMapping("/api/operation/order-cancellations")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun queue(@RequestParam(defaultValue = "0") @Min(0) page: Int, @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int) =
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

    @PatchMapping("/api/operation/order-cancellations/{orderId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun resolve(@PathVariable orderId: UUID, @Valid @RequestBody request: CancellationResolutionRequest) =
        cancellations.resolve(orderId, request.approved, currentAccounts.activeAccountPublicId()).toResponse()

    private fun com.buyeong.umji.api.order.application.model.CancellationChange.toResponse() =
        CancellationChangeResponse(orderId, orderStatus, requestStatus)
}

data class CancellationResolutionRequest(val approved: Boolean)
data class CancellationChangeResponse(val orderId: UUID, val orderStatus: String, val requestStatus: String)
data class CancellationQueueItemResponse(val orderId: UUID, val orderNumber: String, val accountId: UUID, val requestedAt: Instant)
data class CancellationQueueResponse(val items: List<CancellationQueueItemResponse>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)