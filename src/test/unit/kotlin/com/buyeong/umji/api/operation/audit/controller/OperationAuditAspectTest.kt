package com.buyeong.umji.api.operation.audit.controller

import com.buyeong.umji.api.constant.Constant
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.operation.audit.model.OperationAuditEvent
import com.buyeong.umji.api.operation.audit.service.OperationAuditService
import com.buyeong.umji.api.operation.model.OperationAccountResponse
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping

class OperationAuditAspectTest : DescribeSpec({
    val audit = mockk<OperationAuditService>(relaxed = true)
    val transactions = mockk<PlatformTransactionManager>(relaxed = true)
    val aspect = OperationAuditAspect(audit, transactions)
    val actorId = UUID.randomUUID()
    val recorded = slot<OperationAuditEvent>()

    beforeTest {
        clearMocks(audit, transactions)
        every { transactions.getTransaction(any<TransactionDefinition>()) } returns SimpleTransactionStatus()
        every { audit.record(capture(recorded)) } just runs
        val now = Instant.now()
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(actorId.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build()
        SecurityContextHolder.getContext().authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(jwt, "token")
        MDC.put(Constant.KEY_TRACE_ID, "trace-123")
    }

    afterTest {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    fun request(method: String, route: String, variables: Map<String, String> = emptyMap()) {
        val servletRequest = MockHttpServletRequest(method, route.replace(Regex("\\{[^}]+}"), "value"))
        servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, route)
        servletRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(servletRequest))
    }

    describe("성공한 운영 변경 감사 기록") {
        it("계정 변경은 민감한 응답 필드를 로그에 복사하지 않는다") {
            val accountId = UUID.randomUUID()
            val response = OperationAccountResponse(accountId, "개인 이름", "01012345678", "private@example.com", "ACTIVE", 1, null)
            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns response
            request("POST", "/api/operation/accounts")

            aspect.recordSuccessfulChanges(joinPoint) shouldBe response

            recorded.captured.actorId shouldBe actorId
            recorded.captured.action shouldBe "POST /api/operation/accounts"
            recorded.captured.resourceType shouldBe "ACCOUNT"
            recorded.captured.resourceId shouldBe accountId
            recorded.captured.requestTraceId shouldBe "trace-123"
        }

        it("role 변경 로그는 대상 계정과 제한된 role code를 기록한다") {
            val accountId = UUID.randomUUID()
            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns emptyList<Any>()
            request("PUT", "/api/operation/accounts/{id}/roles/{roleCode}", mapOf("id" to accountId.toString(), "roleCode" to "PRODUCT_MANAGER"))

            aspect.recordSuccessfulChanges(joinPoint)

            recorded.captured.resourceType shouldBe "ACCOUNT_ROLE"
            recorded.captured.resourceId shouldBe accountId
            recorded.captured.action shouldBe "PUT /api/operation/accounts/{id}/roles/{roleCode} [PRODUCT_MANAGER]"
        }

        it("카탈로그 생성 응답의 공개 ID를 감사 대상에 사용한다") {
            val productId = UUID.randomUUID()
            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns OperationCatalogResourceResponse(productId)
            request("POST", "/api/operation/products")

            aspect.recordSuccessfulChanges(joinPoint)

            recorded.captured.resourceType shouldBe "PRODUCT"
            recorded.captured.resourceId shouldBe productId
        }

        it("관리자 재고 조정은 SKU를 감사 대상으로 기록한다") {
            val skuId = UUID.randomUUID()
            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } returns InventoryStockResponse(skuId, "SKU-1", 5, 0, 5, 0)
            request("PATCH", "/api/operation/inventory/skus/{skuId}", mapOf("skuId" to skuId.toString()))

            aspect.recordSuccessfulChanges(joinPoint)

            recorded.captured.resourceType shouldBe "INVENTORY"
            recorded.captured.resourceId shouldBe skuId
        }

        it("실패한 운영 변경은 감사 로그를 남기지 않고 트랜잭션을 rollback한다") {
            val joinPoint = mockk<ProceedingJoinPoint>()
            every { joinPoint.proceed() } throws IllegalStateException("write failed")
            request("PATCH", "/api/operation/inventory/skus/{skuId}", mapOf("skuId" to UUID.randomUUID().toString()))

            shouldThrow<IllegalStateException> { aspect.recordSuccessfulChanges(joinPoint) }

            verify(exactly = 0) { audit.record(any()) }
            verify(exactly = 1) { transactions.rollback(any<TransactionStatus>()) }
        }
    }
})
