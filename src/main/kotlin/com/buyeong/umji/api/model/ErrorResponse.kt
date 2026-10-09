package com.buyeong.umji.api.model

import com.buyeong.umji.api.enums.ErrorCode
import com.buyeong.umji.api.model.FieldErrorResponse
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.validation.BindingResult

/**
 * Global Exception Handler에서 발생한 에러에 대한 응답 처리를 관리
 */
@Schema(description = "API 요청 실패 시 반환하는 공통 오류 응답")
data class ErrorResponse(
    @field:Schema(description = "오류 또는 업무 코드", example = "EXAMPLE_CODE", type = "string", required = true)
    val code: ErrorCode,

    @field:Schema(description = "오류 설명", example = "요청 값이 유효하지 않습니다", type = "string", required = false)
    val reason: String? = null,

    @field:Schema(description = "필드별 입력 오류 목록", example = "[]", type = "array", required = false)
    @field:ArraySchema(schema = Schema(implementation = FieldErrorResponse::class)) val errors: List<FieldErrorResponse>? = null,
) {
    private val status: Int = code.status
    private val divisionCode: String = code.divisionCode
    private val resultMessage: String = code.message

    companion object {
        fun of(
            code: ErrorCode,
            bindingResult: BindingResult,
        ): ErrorResponse = ErrorResponse(
            code = code,
            errors = FieldErrorResponse.of(bindingResult),
        )

        fun of(code: ErrorCode): ErrorResponse = ErrorResponse(
            code = code,
        )

        fun of(
            code: ErrorCode,
            reason: String?,
        ): ErrorResponse = ErrorResponse(
            code = code,
            reason = reason,
        )
    }
}