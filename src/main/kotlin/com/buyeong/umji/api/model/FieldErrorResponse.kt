package com.buyeong.umji.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.validation.BindingResult
import java.util.stream.Collectors

@Schema(description = "요청 필드별 유효성 검사 오류 정보")
data class FieldErrorResponse(
    @field:Schema(description = "오류가 발생한 요청 필드명", example = "phone", type = "string", required = true)
    val field: String,

    @field:Schema(description = "검증에 실패한 입력값", example = "예시 값", type = "string", required = true)
    val value: String,

    @field:Schema(description = "오류 설명", example = "요청 값이 유효하지 않습니다", type = "string", required = false)
    val reason: String?,
) {
    companion object {
        fun of(field: String, value: String, reason: String?): List<FieldErrorResponse> =
            listOf(FieldErrorResponse(field, value, reason))

        internal fun of(bindingResult: BindingResult): List<FieldErrorResponse> =
            bindingResult.fieldErrors.stream().map { error ->
                FieldErrorResponse(error.field, error.rejectedValue?.toString().orEmpty(), error.defaultMessage)
            }.collect(Collectors.toList())
    }
}