package com.buyeong.umji.api.domain.account.service

import com.buyeong.umji.api.domain.account.dto.SharedAddressCommandDto
import com.buyeong.umji.api.persistence.jpa.account.service.CustomerAccountJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.mockk
import java.util.UUID

class CustomerAccountServiceTest : DescribeSpec({
    val accounts = mockk<CustomerAccountJpaEntityService>()
    val service = CustomerAccountService(accounts)
    val accountId = UUID.randomUUID()

    describe("공용 배송지 입력") {
        it("필수 수령 정보가 없으면 저장하지 않는다") {
            shouldThrow<IllegalArgumentException> {
                service.createAddress(accountId, SharedAddressCommandDto("", "01012345678", "12345", "서울", null, false))
            }
        }

        it("저장 컬럼 길이를 넘는 입력을 거부한다") {
            shouldThrow<IllegalArgumentException> {
                service.createAddress(accountId, SharedAddressCommandDto("수령인", "01012345678", "12345", "주소".repeat(128), null, false))
            }
        }
    }
})