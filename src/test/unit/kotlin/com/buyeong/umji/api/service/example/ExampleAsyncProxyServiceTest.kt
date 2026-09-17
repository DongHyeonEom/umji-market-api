package com.buyeong.umji.api.service.example

import com.buyeong.umji.api.dto.example.InternalUserDataDto
import com.buyeong.umji.api.dto.example.InternalUserRequestDto
import com.buyeong.umji.api.dto.example.InternalUserResponseDto
import com.buyeong.umji.api.enums.example.RequestStatus
import com.buyeong.umji.api.enums.example.UserStatus
import com.buyeong.umji.api.exception.ApiCallException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/**
 * ExampleAsyncProxyService 단위 테스트.
 *
 * 참고: @Async 어노테이션은 Spring 컨텍스트가 없으면 동기적으로 실행됩니다.
 * 이 테스트에서는 processAsync 메서드가 동기적으로 호출됩니다.
 */
class ExampleAsyncProxyServiceTest : BehaviorSpec({
    val exampleWebClientService = mockk<ExampleWebClientService>()
    val asyncProxyService = ExampleAsyncProxyService(exampleWebClientService)

    Given("비동기 요청 접수") {
        val request =
            InternalUserRequestDto(
                userId = "user123",
                userName = "홍길동",
                email = "hong@example.com",
                phone = "010-1234-5678",
            )

        When("요청을 접수하면") {
            // 외부 API 호출 모킹 (성공 케이스)
            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(
                    success = true,
                    message = "등록 성공",
                    user =
                    InternalUserDataDto(
                        userId = "user123",
                        registeredAt = LocalDateTime.now(),
                        status = UserStatus.ACTIVE,
                    ),
                )

            val requestId = asyncProxyService.submitRequest(request)

            Then("requestId가 반환됨") {
                requestId.shouldNotBeEmpty()
            }

            Then("상태가 조회 가능함") {
                // 참고: @Async가 동기로 실행되므로 이미 처리 완료 상태일 수 있음
                val status = asyncProxyService.getRequestStatus(requestId)
                status.shouldNotBeNull()
                status.userId shouldBe "user123"
            }
        }
    }

    Given("상태 조회") {
        val request =
            InternalUserRequestDto(
                userId = "user456",
                userName = "김철수",
                email = null,
                phone = null,
            )

        When("존재하지 않는 requestId로 조회하면") {
            val status = asyncProxyService.getRequestStatus("non-existent-id")

            Then("null 반환") {
                status.shouldBeNull()
            }
        }

        When("존재하는 requestId로 조회하면") {
            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(success = true, message = "성공", user = null)

            val requestId = asyncProxyService.submitRequest(request)
            val status = asyncProxyService.getRequestStatus(requestId)

            Then("상태 정보가 반환됨") {
                status.shouldNotBeNull()
                status.userId shouldBe "user456"
            }
        }
    }

    Given("요청 취소") {
        When("존재하지 않는 requestId를 취소하면") {
            val cancelResult = asyncProxyService.cancelRequest("non-existent-id")

            Then("false 반환") {
                cancelResult shouldBe false
            }
        }

        When("이미 처리 완료된 요청을 취소하면") {
            val request =
                InternalUserRequestDto(
                    userId = "user-completed",
                    userName = "완료테스트",
                    email = null,
                    phone = null,
                )

            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(success = true, message = "성공", user = null)

            val requestId = asyncProxyService.submitRequest(request)

            // 이미 COMPLETED 상태 (동기 실행으로 인해)
            val cancelResult = asyncProxyService.cancelRequest(requestId)

            Then("false 반환 (처리 완료 후에는 취소 불가)") {
                cancelResult shouldBe false
            }
        }
    }

    Given("백그라운드 처리") {
        val request =
            InternalUserRequestDto(
                userId = "user-async",
                userName = "비동기테스트",
                email = "async@test.com",
                phone = null,
            )

        When("외부 API가 성공하면") {
            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(
                    success = true,
                    message = "등록 성공",
                    user =
                    InternalUserDataDto(
                        userId = "user-async",
                        registeredAt = LocalDateTime.now(),
                        status = UserStatus.ACTIVE,
                    ),
                )

            val requestId = asyncProxyService.submitRequest(request)

            Then("상태가 COMPLETED로 변경됨") {
                val status = asyncProxyService.getRequestStatus(requestId)
                status.shouldNotBeNull()
                status.status shouldBe RequestStatus.COMPLETED
            }

            Then("결과가 저장됨") {
                val result = asyncProxyService.getResult(requestId)
                result.shouldNotBeNull()
                result.success shouldBe true
                result.response?.user?.userId shouldBe "user-async"
            }
        }

        When("외부 API가 비즈니스 실패를 반환하면") {
            val failRequest =
                InternalUserRequestDto(
                    userId = "user-fail",
                    userName = "실패테스트",
                    email = null,
                    phone = null,
                )

            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(
                    success = false,
                    message = "사용자 이미 존재",
                    user = null,
                )

            val requestId = asyncProxyService.submitRequest(failRequest)

            Then("상태가 FAILED로 변경됨") {
                val status = asyncProxyService.getRequestStatus(requestId)
                status.shouldNotBeNull()
                status.status shouldBe RequestStatus.FAILED
            }

            Then("실패 결과가 저장됨") {
                val result = asyncProxyService.getResult(requestId)
                result.shouldNotBeNull()
                result.success shouldBe false
                result.response?.message shouldBe "사용자 이미 존재"
            }
        }

        When("외부 API 호출 중 예외가 발생하면") {
            val errorRequest =
                InternalUserRequestDto(
                    userId = "user-error",
                    userName = "에러테스트",
                    email = null,
                    phone = null,
                )

            every { exampleWebClientService.registerUser(any()) } throws
                ApiCallException("Connection refused")

            val requestId = asyncProxyService.submitRequest(errorRequest)

            Then("상태가 FAILED로 변경됨") {
                val status = asyncProxyService.getRequestStatus(requestId)
                status.shouldNotBeNull()
                status.status shouldBe RequestStatus.FAILED
            }

            Then("재시도 횟수가 증가함") {
                val status = asyncProxyService.getRequestStatus(requestId)
                status?.retryCount shouldBe 1
            }

            Then("에러 결과가 저장됨") {
                val result = asyncProxyService.getResult(requestId)
                result.shouldNotBeNull()
                result.success shouldBe false
            }
        }
    }

    Given("결과 조회") {
        When("처리 완료된 요청의 결과를 조회하면") {
            every { exampleWebClientService.registerUser(any()) } returns
                InternalUserResponseDto(success = true, message = "성공", user = null)

            val request =
                InternalUserRequestDto(
                    userId = "user-result",
                    userName = "결과테스트",
                    email = null,
                    phone = null,
                )
            val requestId = asyncProxyService.submitRequest(request)

            val result = asyncProxyService.getResult(requestId)

            Then("결과가 반환됨") {
                result.shouldNotBeNull()
                result.success shouldBe true
            }
        }

        When("존재하지 않는 requestId로 결과를 조회하면") {
            val result = asyncProxyService.getResult("non-existent-id")

            Then("null 반환") {
                result.shouldBeNull()
            }
        }
    }
})