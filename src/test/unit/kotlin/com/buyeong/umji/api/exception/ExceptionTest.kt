package com.buyeong.umji.api.exception

import com.buyeong.umji.api.enums.ErrorCode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ExceptionTest : DescribeSpec({

    describe("ItemNotFoundException") {
        it("should create with message only") {
            val exception = ItemNotFoundException("Item not found")

            exception.message shouldBe "Item not found"
            exception.code shouldBe null
            exception.replaceValues() shouldBe emptyMap()
        }

        it("should create with code and message") {
            val exception = ItemNotFoundException("ERR001", "User not found")

            exception.message shouldBe "User not found"
            exception.code shouldBe "ERR001"
            exception.replaceValues() shouldBe emptyMap()
        }

        it("should create with code, message, and replace values") {
            val replaceValues = mapOf("userId" to "123", "type" to "admin")
            val exception = ItemNotFoundException("ERR001", "User {userId} not found", replaceValues)

            exception.message shouldBe "User {userId} not found"
            exception.code shouldBe "ERR001"
            exception.replaceValues() shouldBe replaceValues
        }

        it("should implement ExceptionCode interface") {
            val exception = ItemNotFoundException("test")
            exception.shouldBeInstanceOf<ExceptionCode>()
        }
    }

    describe("ClientBadRequestException") {
        it("should create with message only") {
            val exception = ClientBadRequestException("Bad request")

            exception.message shouldBe "Bad request"
            exception.code shouldBe null
            exception.replaceValues() shouldBe emptyMap()
        }

        it("should create with code and message") {
            val exception = ClientBadRequestException("ERR002", "Invalid input")

            exception.message shouldBe "Invalid input"
            exception.code shouldBe "ERR002"
        }

        it("should create with code, message, and replace values") {
            val replaceValues = mapOf("field" to "email")
            val exception = ClientBadRequestException("ERR002", "Invalid {field}", replaceValues)

            exception.replaceValues() shouldBe replaceValues
        }

        it("should implement ExceptionCode interface") {
            val exception = ClientBadRequestException("test")
            exception.shouldBeInstanceOf<ExceptionCode>()
        }
    }

    describe("InvalidRequestParameterException") {
        it("should create with default message") {
            val exception = InvalidRequestParameterException()

            exception.message shouldBe "유효하지 않은 요청 파라미터 입니다."
        }

        it("should create with custom message") {
            val exception = InvalidRequestParameterException("Custom error message")

            exception.message shouldBe "Custom error message"
        }

        it("should create with code and message") {
            val exception = InvalidRequestParameterException("PARAM001", "Invalid parameter value")

            exception.message shouldBe "Invalid parameter value"
            exception.code shouldBe "PARAM001"
        }

        it("should extend ClientBadRequestException") {
            val exception = InvalidRequestParameterException()
            exception.shouldBeInstanceOf<ClientBadRequestException>()
        }
    }

    describe("ApiCallException") {
        it("should create with default message and gateway error code") {
            val exception = ApiCallException()

            exception.message shouldBe "외부 API 호출에 실패했습니다."
            exception.errorCode shouldBe ErrorCode.BAD_GATEWAY_ERROR
        }

        it("should create with custom message") {
            val exception = ApiCallException("Custom API error")

            exception.message shouldBe "Custom API error"
            exception.errorCode shouldBe ErrorCode.BAD_GATEWAY_ERROR
        }

        it("should create with message and error code") {
            val exception = ApiCallException("Service configuration is missing", ErrorCode.INTERNAL_SERVER_ERROR)

            exception.message shouldBe "Service configuration is missing"
            exception.errorCode shouldBe ErrorCode.INTERNAL_SERVER_ERROR
        }

        it("should create with message, error code, and cause") {
            val cause = RuntimeException("Original error")
            val exception = ApiCallException("API failed", ErrorCode.BAD_GATEWAY_ERROR, cause)

            exception.message shouldBe "API failed"
            exception.errorCode shouldBe ErrorCode.BAD_GATEWAY_ERROR
            exception.cause shouldBe cause
        }
    }
})
