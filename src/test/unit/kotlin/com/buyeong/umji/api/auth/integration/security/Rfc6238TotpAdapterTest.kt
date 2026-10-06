package com.buyeong.umji.api.auth.integration.security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class Rfc6238TotpServiceTest : DescribeSpec({
    val adapter = Rfc6238TotpService(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC))

    describe("RFC 6238 TOTP") {
        it("RFC HMAC-SHA1 test secret에 대한 6자리 코드를 검증한다") {
            adapter.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "287082") shouldBe true
            adapter.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "287083") shouldBe false
        }

        it("Authenticator 호환 URI에 앱 이름과 secret을 포함한다") {
            adapter.provisioningUri("admin", "JBSWY3DPEHPK3PXP") shouldBe
                "otpauth://totp/Umji+Market%3Aadmin?secret=JBSWY3DPEHPK3PXP&issuer=Umji%20Market&algorithm=SHA1&digits=6&period=30"
        }
    }
})