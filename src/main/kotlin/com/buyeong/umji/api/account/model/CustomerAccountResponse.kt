package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.model.CustomerProfile
import com.buyeong.umji.api.account.model.SharedAddress
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "CustomerProfileResponse API 데이터 모델")
data class CustomerProfileResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true) val name: String,
    @field:Schema(description = "휴대폰 번호", example = "01012345678", type = "string", required = true) val phone: String?,
    @field:Schema(description = "이메일 주소", example = "user@example.com", type = "string", required = true) val email: String?,
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
)

@Schema(description = "SharedAddressResponse API 데이터 모델")
data class SharedAddressResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "배송 수령인 이름", example = "예시 값", type = "string", required = true) val recipientName: String,
    @field:Schema(description = "배송 수령인 휴대폰 번호", example = "예시 값", type = "string", required = true) val recipientPhone: String,
    @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = true) val postalCode: String,
    @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = true) val address1: String,
    @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = true) val address2: String?,
    @field:Schema(description = "Is Default 정보", example = "true", type = "boolean", required = true, implementation = Boolean::class) val isDefault: Boolean,
    @field:Schema(description = "생성 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val createdAt: Instant,
    @field:Schema(description = "마지막 수정 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val updatedAt: Instant,
)

fun CustomerProfile.toResponse() = CustomerProfileResponse(id, name, phone, email, status)

fun SharedAddress.toResponse() = SharedAddressResponse(
    id,
    recipientName,
    recipientPhone,
    postalCode,
    address1,
    address2,
    isDefault,
    createdAt,
    updatedAt,
)
