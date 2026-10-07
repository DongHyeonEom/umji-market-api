package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
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
    @field:Schema(description = "세금계산서 발행 정보 snapshot. 미발행 주문은 null", type = "object", required = true) val taxInvoiceSnapshot: TaxInvoiceSnapshotResponse?,
    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", required = true) val channelCode: String = "WHOLESALE",
    @field:Schema(description = "판매 Organization 공개 식별자", format = "uuid", type = "string") val sellerOrganizationId: UUID? = null,
)

@Schema(description = "판매 Organization별로 분리 생성된 주문 목록")
data class OrderCheckoutResponse(
    @field:ArraySchema(schema = Schema(implementation = OrderResponse::class))
    val orders: List<OrderResponse>,
)

@Schema(description = "주문 입금에 사용할 은행 계좌 안내 정보")
data class BankAccountInstructionsResponse(
    @field:Schema(description = "입금 은행명", example = "예시 값", type = "string", required = true) val bankName: String,
    @field:Schema(description = "입금 계좌번호", example = "예시 값", type = "string", required = true) val accountNumber: String,
    @field:Schema(description = "예금주명", example = "예시 값", type = "string", required = true) val accountHolder: String,
)

@Schema(description = "세금계산서 설정과 주문 유형별 입금 계좌 정보")
data class OrderCheckoutOptionsResponse(
    @field:Schema(description = "Organization 기본 세금계산서 발행 설정", example = "true", type = "boolean", required = true, implementation = Boolean::class) val defaultTaxInvoiceRequested: Boolean,
    @field:Schema(
        description = "공급자와 그룹 공급받는자 필수 정보가 완성되어 발행 요청할 수 있는지 여부",
        example = "true",
        type = "boolean",
        required = true,
        implementation = Boolean::class,
    ) val taxInvoiceAvailable: Boolean,
    @field:Schema(description = "Standard Bank Account 정보", example = "예시 값", type = "object", required = true) val standardBankAccount: BankAccountInstructionsResponse,
    @field:Schema(description = "Tax Invoice Bank Account 정보", example = "예시 값", type = "object", required = true) val taxInvoiceBankAccount: BankAccountInstructionsResponse,
)

@Schema(description = "배송지와 세금계산서 설정을 포함한 주문 생성 요청")
data class CreateOrderRequest(
    @field:Schema(description = "저장된 배송지 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val shippingAddressId:
    UUID,
    @field:Schema(description = "세금계산서 발행 요청 여부", example = "true", type = "boolean", required = false, implementation = Boolean::class) val taxInvoiceRequested: Boolean? = null,
    @field:Schema(
        description = "대표자가 Organization 기본 세금계산서 설정을 갱신할지 여부",
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
    @field:Schema(description = "판매 단위 1개의 주문 당시 가격(원). WHOLESALE은 박스당 가격", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class) val unitPrice: Long,
    @field:Schema(description = "판매 단위 주문 수량. WHOLESALE은 박스 수, RETAIL은 낱개 수", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
    @field:Schema(description = "수량을 반영한 항목 금액(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val lineAmount: Long,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
    @field:Schema(description = "채널별 판매 오퍼 공개 UUID") val salesOfferId: UUID? = null,
    @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량",
        example = "12",
        type = "integer",
        required = true,
        implementation = Int::class,
    ) val unitsPerSale: Int = 1,
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

@Schema(description = "송장 등록 시점의 공급자·공급받는자·품목별 세금계산서 발행 정보")
data class TaxInvoiceSnapshotResponse(
    @field:Schema(description = "세금계산서 상태", example = "READY_FOR_ISSUANCE", type = "string", required = true) val status: String,
    @field:Schema(description = "작성일자(KST). 송장 등록 시 주문일자로 설정", example = "2026-10-06", format = "date", type = "string", required = true) val writtenDate: LocalDate?,
    @field:Schema(description = "제공일자(KST). 송장 등록 시 주문일자로 설정", example = "2026-10-06", format = "date", type = "string", required = true) val supplyDate: LocalDate?,
    @field:Schema(description = "공급자 사업자등록번호", example = "123-45-67890", type = "string", required = true) val supplierBusinessRegistrationNumber: String,
    @field:Schema(description = "공급자 상호", example = "엄지마켓", type = "string", required = true) val supplierBusinessName: String,
    @field:Schema(description = "공급자 성명", example = "홍길동", type = "string", required = true) val supplierName: String,
    @field:Schema(description = "공급자 사업장주소", example = "서울특별시 강남구 테헤란로 1", type = "string", required = true) val supplierAddress: String,
    @field:Schema(description = "공급자 업태", example = "도소매업", type = "string", required = true) val supplierIndustry: String,
    @field:Schema(description = "공급자 종목", example = "철물·공구", type = "string", required = true) val supplierItem: String,
    @field:Schema(description = "공급자 이메일", example = "seller@example.com", type = "string", required = true) val supplierEmail: String,
    @field:Schema(description = "공급받는자 사업자등록번호", example = "987-65-43210", type = "string", required = true) val buyerBusinessRegistrationNumber: String,
    @field:Schema(description = "공급받는자 상호", example = "엄지상사", type = "string", required = true) val buyerBusinessName: String,
    @field:Schema(description = "공급받는자 성명", example = "김철수", type = "string", required = true) val buyerName: String,
    @field:Schema(description = "공급받는자 우편번호", example = "06234", type = "string", required = true) val buyerPostalCode: String,
    @field:Schema(description = "공급받는자 사업자주소 기본 주소", example = "서울특별시 강남구 테헤란로 2", type = "string", required = true) val buyerAddress1: String,
    @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "202호", type = "string", required = true) val buyerAddress2: String?,
    @field:Schema(description = "공급받는자 업태", example = "도소매업", type = "string", required = true) val buyerIndustry: String,
    @field:Schema(description = "공급받는자 종목", example = "철물·공구", type = "string", required = true) val buyerItem: String,
    @field:Schema(description = "선택 공급받는자 이메일", example = "billing@example.com", type = "string", required = true) val buyerEmail: String?,
    @field:ArraySchema(
        schema = Schema(implementation = TaxInvoiceItemResponse::class),
    ) @field:Schema(description = "주문 품목별 공급가액 목록", example = "[]", type = "array", required = true) val items: List<TaxInvoiceItemResponse>,
    @field:Schema(description = "공급가액 합계(원)", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class) val supplyAmount: Long,
)

@Schema(description = "세금계산서에 기재할 주문 품목과 공급가액")
data class TaxInvoiceItemResponse(
    @field:Schema(description = "품목명", example = "철물 상품 규격 A", type = "string", required = true) val itemName: String,
    @field:Schema(description = "주문 당시 SKU 코드", example = "SKU-001", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "주문 수량", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
    @field:Schema(description = "공급가액(원), 주문 항목 lineAmount", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class) val supplyAmount:
    Long,
)
