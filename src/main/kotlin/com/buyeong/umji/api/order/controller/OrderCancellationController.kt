package com.buyeong.umji.api.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.order.service.OrderCancellationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID
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

    private fun com.buyeong.umji.api.order.model.CancellationChange.toResponse() =
        CancellationChangeResponse(orderId, orderStatus, requestStatus)
}

@Schema(description = "운영자의 주문 취소 요청 처리 결과")
data class CancellationResolutionRequest(
    @field:Schema(description = "취소 요청 승인 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val approved: Boolean,
)

@Schema(description = "주문 취소 요청 처리 후 주문 및 요청 상태")
data class CancellationChangeResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,
    @field:Schema(description = "현재 주문 상태 코드", example = "CANCELLED", type = "string", required = true)
    val orderStatus: String,
    @field:Schema(description = "취소 요청 처리 상태 코드", example = "APPROVED", type = "string", required = true)
    val requestStatus: String,
)

@Schema(description = "운영자 처리를 기다리는 주문 취소 요청")
data class CancellationQueueItemResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,
    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true)
    val orderNumber: String,
    @field:Schema(description = "주문자 계정 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val accountId: UUID,
    @field:Schema(description = "취소 요청 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val requestedAt: Instant,
)

@Schema(description = "주문 취소 요청 검색 결과와 페이지 정보")
data class CancellationQueueResponse(
    @field:ArraySchema(
        schema = Schema(implementation = CancellationQueueItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 취소 요청 목록", example = "[]", type = "array", required = true)
    val items: List<CancellationQueueItemResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "0", format = "int32", type = "integer", required = true, implementation = Int::class)
    val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "20", format = "int32", type = "integer", required = true, implementation = Int::class)
    val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val totalPages: Int,
)
