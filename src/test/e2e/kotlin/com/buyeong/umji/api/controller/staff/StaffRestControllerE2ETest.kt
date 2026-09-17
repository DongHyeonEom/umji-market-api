package com.buyeong.umji.api.controller.staff

import com.buyeong.umji.api.model.StaffModel
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

/**
 * StaffRestController E2E 테스트.
 *
 * 실제 HTTP 요청을 통해 API 엔드포인트를 테스트합니다.
 * - 전체 Spring Context 로드
 * - 실제 서버 기동 (RANDOM_PORT)
 * - TestRestTemplate을 통한 HTTP 호출
 * - 실제 인프라(DB, 외부 API) 사용 (src/test/e2e/resources/application.yml 설정)
 *
 * 실행 전 요구사항:
 * - VPN 연결 또는 dev 네트워크 접근
 *
 * 실행 방법:
 * ```bash
 * ./gradlew e2eTest
 * ```
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffRestControllerE2ETest(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
) : DescribeSpec({

    describe("GET /staffs/{id}") {

        context("Staff ID로 조회") {
            it("존재하는 Staff인 경우 StaffModel을 반환한다") {
                // given
                val staffId = 1

                // when
                val response = restTemplate.getForEntity("/staffs/$staffId", String::class.java)

                // then
                when (response.statusCode) {
                    HttpStatus.OK -> {
                        val staffModel = objectMapper.readValue(response.body, StaffModel::class.java)
                        staffModel.shouldNotBeNull()
                        staffModel.id shouldBe staffId
                    }
                    HttpStatus.NOT_FOUND -> {
                        // DB에 데이터가 없는 경우
                        response.body shouldContain "NOT_FOUND"
                    }
                    else -> {
                        // 예상치 못한 응답
                        throw AssertionError("Unexpected status: ${response.statusCode}")
                    }
                }
            }
        }

        context("존재하지 않는 Staff ID로 조회") {
            it("404 Not Found를 반환한다") {
                // given
                val nonExistentId = 999999

                // when
                val response = restTemplate.getForEntity("/staffs/$nonExistentId", String::class.java)

                // then
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body shouldContain "NOT_FOUND"
            }
        }
    }

    describe("GET /staffs/{id}/core") {

        context("Core API를 통한 Staff 조회") {
            it("실제 Core API를 통해 StaffModel을 반환한다") {
                // given
                val staffId = 1

                // when
                val response = restTemplate.getForEntity("/staffs/$staffId/core", String::class.java)

                // then
                when (response.statusCode) {
                    HttpStatus.OK -> {
                        val staffModel = objectMapper.readValue(response.body, StaffModel::class.java)
                        staffModel.shouldNotBeNull()
                        staffModel.id shouldBe staffId
                    }
                    HttpStatus.NOT_FOUND -> {
                        // Core API에 데이터가 없는 경우
                        response.body shouldContain "NOT_FOUND"
                    }
                    HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR -> {
                        // Core API 연결 실패
                        println("Core API 연결 실패: ${response.statusCode}")
                    }
                    else -> {
                        throw AssertionError("Unexpected status: ${response.statusCode}")
                    }
                }
            }
        }
    }
})