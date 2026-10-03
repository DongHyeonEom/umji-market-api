package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "SharedAddressRequest API 데이터 모델")
data class SharedAddressRequest(
    @field:NotBlank @field:Size(max = 100) @field:Schema(description = "배송 수령인 이름", example = "예시 값", type = "string", required = true) val recipientName: String,
    @field:NotBlank @field:Size(max = 30) @field:Schema(description = "배송 수령인 휴대폰 번호", example = "예시 값", type = "string", required = true) val recipientPhone: String,
    @field:NotBlank @field:Size(max = 20) @field:Schema(description = "우편번호", example = "예시 값", type = "string", required = true) val postalCode: String,
    @field:NotBlank @field:Size(max = 255) @field:Schema(description = "기본 주소", example = "예시 값", type = "string", required = true) val address1: String,
    @field:Size(max = 255) @field:Schema(description = "상세 주소", example = "예시 값", type = "string", required = false) val address2: String? = null,
    @field:Schema(description = "Is Default 정보", example = "true", type = "boolean", required = false, implementation = Boolean::class) val isDefault: Boolean = false,
)

fun SharedAddressRequest.toCommand() = SharedAddressCommand(
    recipientName.trim(),
    recipientPhone.trim(),
    postalCode.trim(),
    address1.trim(),
    address2?.trim()?.takeIf(String::isNotEmpty),
    isDefault,
)