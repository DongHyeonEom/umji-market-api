package com.buyeong.umji.api.exception

import com.buyeong.umji.api.enums.ErrorCode

/**
 * 외부 API 호출 오류를 공통 ErrorCode와 함께 전달하는 예외
 */
class ApiCallException(
    message: String = "외부 API 호출에 실패했습니다.",
    val errorCode: ErrorCode = ErrorCode.BAD_GATEWAY_ERROR,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
