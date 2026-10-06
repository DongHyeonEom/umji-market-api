package com.buyeong.umji.api.model

import com.buyeong.umji.api.enums.ErrorCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.stream.Collectors
import org.springframework.validation.BindingResult

/**
 * Global Exception Handler에서 발생한 에러에 대한 응답 처리를 관리
 */
@Schema(description = "API 요청 실패 시 반환하는 공통 오류 응답")
data class ErrorResponseModel(
    @field:Schema(description = "오류 또는 업무 코드", example = "EXAMPLE_CODE", type = "string", required = true)
    val code: ErrorCode,
    @field:Schema(description = "오류 설명", example = "요청 값이 유효하지 않습니다", type = "string", required = false)
    val reason: String? = null,
    @field:Schema(description = "필드별 입력 오류 목록", example = "[]", type = "array", required = false)
    @field:ArraySchema(schema = Schema(implementation = FieldError::class)) val errors: List<FieldError>? = null,
) {
    private val status: Int = code.status
    private val divisionCode: String = code.divisionCode
    private val resultMessage: String = code.message

    // 에러를 e.getBindingResult() 형태로 전달 받는 경우 해당 내용을 상세 내용으로 변경하는 기능을 수행한다.
    @Schema(description = "요청 필드별 유효성 검사 오류 정보")
    data class FieldError(
        @field:Schema(description = "오류가 발생한 요청 필드명", example = "phone", type = "string", required = true)
        val field: String,
        @field:Schema(description = "검증에 실패한 입력값", example = "예시 값", type = "string", required = true)
        val value: String,
        @field:Schema(description = "오류 설명", example = "요청 값이 유효하지 않습니다", type = "string", required = false)
        val reason: String?,
    ) {
        companion object {
            fun of(
                field: String,
                value: String,
                reason: String?,
            ): List<FieldError> {
                val fieldErrors: MutableList<FieldError> = ArrayList()
                fieldErrors.add(FieldError(field, value, reason))
                return fieldErrors
            }

            internal fun of(bindingResult: BindingResult): List<FieldError> {
                val fieldErrors = bindingResult.fieldErrors

                return fieldErrors.stream().map { error: org.springframework.validation.FieldError ->
                    FieldError(
                        error.field,
                        if (error.rejectedValue == null) {
                            ""
                        } else {
                            error.rejectedValue.toString()
                        },
                        error.defaultMessage,
                    )
                }.collect(Collectors.toList())
            }
        }
    }

    companion object {
        fun of(
            code: ErrorCode,
            bindingResult: BindingResult,
        ): ErrorResponseModel = ErrorResponseModel(
            code = code,
            errors = FieldError.of(bindingResult),
        )

        fun of(code: ErrorCode): ErrorResponseModel = ErrorResponseModel(
            code = code,
        )

        fun of(
            code: ErrorCode,
            reason: String?,
        ): ErrorResponseModel = ErrorResponseModel(
            code = code,
            reason = reason,
        )
    }
}
