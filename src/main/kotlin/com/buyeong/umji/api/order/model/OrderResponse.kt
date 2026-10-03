package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "주문 당시 상품·가격·결제·배송 정보를 포함하는 주문 응답")
data class OrderResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true) val orderNumber: String,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
    @field:Schema(description = "상품 소계(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val subtotalAmount: Long,
    @field:Schema(description = "최종 결제 금액(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalAmount: Long,
    @field:Schema(description = "주문 생성 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val orderedAt: Instant,
    @field:ArraySchema(
        schema = Schema(implementation = OrderItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<OrderItemResponse>,
    @field:Schema(description = "결제 수단 코드", example = "BANK_TRANSFER", type = "string", required = true) val paymentMethod: String,
    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true) val paymentStatus: String,
    @field:Schema(description = "세금계산서 발행 요청 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class) val taxInvoiceRequested: Boolean,
    @field:Schema(description = "Deposit Bank Name 정보", example = "예시 값", type = "string", required = true) val depositBankName: String?,
    @field:Schema(description = "Deposit Account Number 정보", example = "예시 값", type = "string", required = true) val depositAccountNumber: String?,
    @field:Schema(description = "Deposit Account Holder 정보", example = "예시 값", type = "string", required = true) val depositAccountHolder: String?,
    @field:Schema(description = "배송 상태 코드", example = "PREPARING", type = "string", required = true) val shippingStatus: String,
    @field:Schema(description = "택배사 코드", example = "예시 값", type = "string", required = true) val carrierCode: String?,
    @field:Schema(description = "택배 송장 번호", example = "예시 값", type = "string", required = true) val trackingNumber: String?,
    @field:Schema(description = "택배사 배송 조회 URL", example = "예시 값", type = "string", required = true) val trackingUrl: String?,
    @field:Schema(description = "Cancellation Request Status 정보", example = "예시 값", type = "string", required = true) val cancellationRequestStatus: String?,
    @field:Schema(description = "주문자 이름", example = "예시 값", type = "string", required = true) val orderedByName: String?,
    @field:Schema(description = "주문자 휴대폰 번호 뒤 네 자리", example = "예시 값", type = "string", required = true) val orderedByPhoneSuffix: String?,
    @field:Schema(description = "배송 수령인 이름", example = "예시 값", type = "string", required = true) val shippingRecipientName: String?,
    @field:Schema(description = "배송 수령인 휴대폰 번호", example = "예시 값", type = "string", required = true) val shippingRecipientPhone: String?,
    @field:Schema(description = "배송지 우편번호", example = "예시 값", type = "string", required = true) val shippingPostalCode: String?,
    @field:Schema(description = "배송지 기본 주소", example = "예시 값", type = "string", required = true) val shippingAddress1: String?,
    @field:Schema(description = "배송지 상세 주소", example = "예시 값", type = "string", required = true) val shippingAddress2: String?,
)

@Schema(description = "주문 입금에 사용할 은행 계좌 안내 정보")
data class BankAccountInstructionsResponse(
    @field:Schema(description = "입금 은행명", example = "예시 값", type = "string", required = true) val bankName: String,
    @field:Schema(description = "입금 계좌번호", example = "예시 값", type = "string", required = true) val accountNumber: String,
    @field:Schema(description = "예금주명", example = "예시 값", type = "string", required = true) val accountHolder: String,
)

@Schema(description = "세금계산서 설정과 주문 유형별 입금 계좌 정보")
data class OrderCheckoutOptionsResponse(
    @field:Schema(description = "계정 기본 세금계산서 발행 설정", example = "true", type = "boolean", required = true, implementation = Boolean::class) val defaultTaxInvoiceRequested: Boolean,
    @field:Schema(description = "Standard Bank Account 정보", example = "예시 값", type = "object", required = true) val standardBankAccount: BankAccountInstructionsResponse,
    @field:Schema(description = "Tax Invoice Bank Account 정보", example = "예시 값", type = "object", required = true) val taxInvoiceBankAccount: BankAccountInstructionsResponse,
)

@Schema(description = "배송지와 세금계산서 설정을 포함한 주문 생성 요청")
data class CreateOrderRequest(
    @field:Schema(description = "저장된 배송지 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val shippingAddressId:
    UUID,
    @field:Schema(description = "세금계산서 발행 요청 여부", example = "true", type = "boolean", required = false, implementation = Boolean::class) val taxInvoiceRequested: Boolean? = null,
    @field:Schema(
        description = "계정 기본 세금계산서 설정 갱신 여부",
        example = "true",
        type = "boolean",
        required = false,
        implementation = Boolean::class,
    ) val updateDefaultTaxInvoicePreference: Boolean = false,
)

@Schema(description = "주문 상품의 SKU·주문 당시 가격·수량 정보")
data class OrderItemResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val skuId: UUID,
    @field:Schema(description = "상품명", example = "예시 값", type = "string", required = true) val productName: String,
    @field:Schema(description = "상품 옵션명", example = "예시 값", type = "string", required = true) val skuName: String,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "상품 한 개의 가격(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val unitPrice: Long,
    @field:Schema(description = "수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
    @field:Schema(description = "수량을 반영한 항목 금액(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val lineAmount: Long,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
)

@Schema(description = "주문 검색 결과 목록과 페이지 정보")
data class OrderPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = OrderResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<OrderResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val totalPages: Int,
)