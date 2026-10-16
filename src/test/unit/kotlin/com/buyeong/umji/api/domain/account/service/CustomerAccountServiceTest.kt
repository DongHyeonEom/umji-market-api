package com.buyeong.umji.api.domain.account.service

import com.buyeong.umji.api.domain.account.dto.SharedAddressCommandDto
import com.buyeong.umji.api.domain.account.dto.SharedAddressDto
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID

class CustomerAccountServiceTest : DescribeSpec({
    val accounts = mockk<CustomerAccountJpaEntityService>()
    val service = CustomerAccountService(accounts)
    val accountId = UUID.randomUUID()

    describe("공용 배송지 입력") {
        it("입력 검증이 끝난 배송지 명령을 저장소로 전달한다") {
            val command = SharedAddressCommandDto("수령인", "01012345678", "12345", "서울", null, false)
            val result =
                SharedAddressDto(
                    UUID.randomUUID(),
                    command.recipientName,
                    command.recipientPhone,
                    command.postalCode,
                    command.address1,
                    command.address2,
                    command.isDefault,
                    Instant.EPOCH,
                    Instant.EPOCH,
                )
            every { accounts.createAddress(accountId, command) } returns result

            service.createAddress(accountId, command) shouldBe result

            verify(exactly = 1) { accounts.createAddress(accountId, command) }
        }
    }
})