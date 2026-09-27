package com.buyeong.umji.api.operation.audit.application

import com.buyeong.umji.api.operation.audit.application.model.OperationAuditPage
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditQuery
import com.buyeong.umji.api.operation.audit.application.port.out.OperationAuditPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class OperationAuditServiceTest : DescribeSpec({
    val port = mockk<OperationAuditPort>(relaxed = true)
    val service = OperationAuditService(port)

    beforeTest {
        clearMocks(port)
    }

    describe("운영 감사 로그 조회") {
        it("범위와 페이지가 유효하면 조회를 위임한다") {
            val query = OperationAuditQuery(null, "PRODUCT", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), 0, 20)
            val result = OperationAuditPage(emptyList(), 0, 20, 0, 0)
            every { port.search(query) } returns result

            service.search(query) shouldBe result

            verify(exactly = 1) { port.search(query) }
        }

        it("조회 시작 시각이 종료 시각보다 늦으면 거부한다") {
            val query = OperationAuditQuery(null, null, Instant.parse("2026-01-02T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), 0, 20)

            shouldThrow<IllegalArgumentException> { service.search(query) }

            verify(exactly = 0) { port.search(any()) }
        }

        it("허용하지 않은 리소스 유형 필터를 거부한다") {
            val query = OperationAuditQuery(null, "ACCOUNT_PHONE", null, null, 0, 20)

            shouldThrow<IllegalArgumentException> { service.search(query) }

            verify(exactly = 0) { port.search(any()) }
        }
    }

    describe("보존 만료 삭제") {
        it("삭제 batch 최대 크기를 제한한다") {
            shouldThrow<IllegalArgumentException> { service.purgeExpired(Instant.now(), 10_001) }

            verify(exactly = 0) { port.purgeExpired(any(), any()) }
        }
    }
})