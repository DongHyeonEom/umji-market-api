package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "송장 등록 시점의 공급자·공급받는자·품목별 세금계산서 발행 정보")
data class TaxInvoiceSnapshotResponse(
    @field:Schema(description = "세금계산서 상태", example = "READY_FOR_ISSUANCE", type = "string", required = true)
    val status: String,

    @field:Schema(description = "작성일자(KST). 송장 등록 시 주문일자로 설정", example = "2026-10-06", format = "date", type = "string", required = true)
    val writtenDate: LocalDate?,

    @field:Schema(description = "제공일자(KST). 송장 등록 시 주문일자로 설정", example = "2026-10-06", format = "date", type = "string", required = true)
    val supplyDate: LocalDate?,

    @field:Schema(description = "공급자 사업자등록번호", example = "123-45-67890", type = "string", required = true)
    val supplierBusinessRegistrationNumber: String,

    @field:Schema(description = "공급자 상호", example = "엄지마켓", type = "string", required = true)
    val supplierBusinessName: String,

    @field:Schema(description = "공급자 성명", example = "홍길동", type = "string", required = true)
    val supplierName: String,

    @field:Schema(description = "공급자 사업장주소", example = "서울특별시 강남구 테헤란로 1", type = "string", required = true)
    val supplierAddress: String,

    @field:Schema(description = "공급자 업태", example = "도소매업", type = "string", required = true)
    val supplierIndustry: String,

    @field:Schema(description = "공급자 종목", example = "철물·공구", type = "string", required = true)
    val supplierItem: String,

    @field:Schema(description = "공급자 이메일", example = "seller@example.com", type = "string", required = true)
    val supplierEmail: String,

    @field:Schema(description = "공급받는자 사업자등록번호", example = "987-65-43210", type = "string", required = true)
    val buyerBusinessRegistrationNumber: String,

    @field:Schema(description = "공급받는자 상호", example = "엄지상사", type = "string", required = true)
    val buyerBusinessName: String,

    @field:Schema(description = "공급받는자 성명", example = "김철수", type = "string", required = true)
    val buyerName: String,

    @field:Schema(description = "공급받는자 우편번호", example = "06234", type = "string", required = true)
    val buyerPostalCode: String,

    @field:Schema(description = "공급받는자 사업자주소 기본 주소", example = "서울특별시 강남구 테헤란로 2", type = "string", required = true)
    val buyerAddress1: String,

    @field:Schema(description = "공급받는자 사업자주소 상세 주소", example = "202호", type = "string", required = true)
    val buyerAddress2: String?,

    @field:Schema(description = "공급받는자 업태", example = "도소매업", type = "string", required = true)
    val buyerIndustry: String,

    @field:Schema(description = "공급받는자 종목", example = "철물·공구", type = "string", required = true)
    val buyerItem: String,

    @field:Schema(description = "선택 공급받는자 이메일", example = "billing@example.com", type = "string", required = true)
    val buyerEmail: String?,

    @field:ArraySchema(
        schema = Schema(implementation = TaxInvoiceItemResponse::class),
    ) @field:Schema(description = "주문 품목별 공급가액 목록", example = "[]", type = "array", required = true)
    val items: List<TaxInvoiceItemResponse>,

    @field:Schema(description = "공급가액 합계(원)", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class)
    val supplyAmount: Long,

    @field:Schema(description = "홈택스 발행 승인번호", type = "string")
    val approvalNumber: String? = null,

    @field:Schema(description = "실제 발행일자", format = "date", type = "string")
    val issuedAt: LocalDate? = null,

    @field:Schema(description = "세액(원)", format = "int64", type = "integer", implementation = Long::class)
    val taxAmount: Long? = null,

    @field:Schema(description = "세금계산서 합계 금액(원)", format = "int64", type = "integer", implementation = Long::class)
    val totalAmount: Long? = null,
)