package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "주문 당시 상품·가격·결제·배송 정보를 포함하는 주문 응답")
data class OrderResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true)
    val orderNumber: String,

    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,

    @field:Schema(description = "상품 소계(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val subtotalAmount: Long,

    @field:Schema(description = "최종 결제 금액(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val totalAmount: Long,

    @field:Schema(description = "주문 생성 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val orderedAt: Instant,

    @field:ArraySchema(
        schema = Schema(implementation = OrderItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true)
    val items: List<OrderItemResponse>,

    @field:Schema(description = "결제 수단 코드", example = "BANK_TRANSFER", type = "string", required = true)
    val paymentMethod: String,

    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true)
    val paymentStatus: String,

    @field:Schema(description = "세금계산서 발행 요청 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val taxInvoiceRequested: Boolean,

    @field:Schema(description = "Deposit Bank Name 정보", example = "예시 값", type = "string", required = true)
    val depositBankName: String?,

    @field:Schema(description = "Deposit Account Number 정보", example = "예시 값", type = "string", required = true)
    val depositAccountNumber: String?,

    @field:Schema(description = "Deposit Account Holder 정보", example = "예시 값", type = "string", required = true)
    val depositAccountHolder: String?,

    @field:Schema(description = "배송 상태 코드", example = "PREPARING", type = "string", required = true)
    val shippingStatus: String,

    @field:Schema(description = "택배사 코드", example = "예시 값", type = "string", required = true)
    val carrierCode: String?,

    @field:Schema(description = "택배 송장 번호", example = "예시 값", type = "string", required = true)
    val trackingNumber: String?,

    @field:Schema(description = "택배사 배송 조회 URL", example = "예시 값", type = "string", required = true)
    val trackingUrl: String?,

    @field:Schema(description = "Cancellation Request Status 정보", example = "예시 값", type = "string", required = true)
    val cancellationRequestStatus: String?,

    @field:Schema(description = "주문자 이름", example = "예시 값", type = "string", required = true)
    val orderedByName: String?,

    @field:Schema(description = "주문자 휴대폰 번호 뒤 네 자리", example = "예시 값", type = "string", required = true)
    val orderedByPhoneSuffix: String?,

    @field:Schema(description = "배송 수령인 이름", example = "예시 값", type = "string", required = true)
    val shippingRecipientName: String?,

    @field:Schema(description = "배송 수령인 휴대폰 번호", example = "예시 값", type = "string", required = true)
    val shippingRecipientPhone: String?,

    @field:Schema(description = "배송지 우편번호", example = "예시 값", type = "string", required = true)
    val shippingPostalCode: String?,

    @field:Schema(description = "배송지 기본 주소", example = "예시 값", type = "string", required = true)
    val shippingAddress1: String?,

    @field:Schema(description = "배송지 상세 주소", example = "예시 값", type = "string", required = true)
    val shippingAddress2: String?,

    @field:Schema(description = "세금계산서 발행 정보 snapshot. 미발행 주문은 null", type = "object", required = true)
    val taxInvoiceSnapshot: TaxInvoiceSnapshotResponse?,

    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", required = true)
    val channelCode: String = "WHOLESALE",

    @field:Schema(description = "판매 Organization 공개 식별자", format = "uuid", type = "string")
    val sellerOrganizationId: UUID? = null,
)