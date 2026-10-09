package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.dto.CustomerProfileDto
import com.buyeong.umji.api.account.dto.SharedAddressDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "SharedAddressResponse API 데이터 모델")
data class SharedAddressResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "배송 수령인 이름", example = "예시 값", type = "string", required = true)
    val recipientName: String,

    @field:Schema(description = "배송 수령인 휴대폰 번호", example = "예시 값", type = "string", required = true)
    val recipientPhone: String,

    @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = true)
    val postalCode: String,

    @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = true)
    val address1: String,

    @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = true)
    val address2: String?,

    @field:Schema(description = "Is Default 정보", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val isDefault: Boolean,

    @field:Schema(description = "생성 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val createdAt: Instant,

    @field:Schema(description = "마지막 수정 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val updatedAt: Instant,
)

fun CustomerProfileDto.toResponse() = CustomerProfileResponse(id, name, phone, email, status)

fun SharedAddressDto.toResponse() = SharedAddressResponse(
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