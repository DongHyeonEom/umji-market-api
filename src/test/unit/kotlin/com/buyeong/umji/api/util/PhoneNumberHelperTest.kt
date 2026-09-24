package com.buyeong.umji.api.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PhoneNumberHelperTest : DescribeSpec({

    describe("PhoneNumberHelper.normalizeMobilePhoneNumber") {
        it("하이픈과 국가 코드를 제거한 휴대폰 번호를 반환한다") {
            PhoneNumberHelper.normalizeMobilePhoneNumber("010-1234-5678") shouldBe "01012345678"
            PhoneNumberHelper.normalizeMobilePhoneNumber("+82 10 1234 5678") shouldBe "01012345678"
            PhoneNumberHelper.normalizeMobilePhoneNumber("0821012345678") shouldBe "01012345678"
        }

        it("휴대폰 번호가 아닌 값은 거부한다") {
            shouldThrow<IllegalArgumentException> {
                PhoneNumberHelper.normalizeMobilePhoneNumber("02-1234-5678")
            }
        }
    }

    describe("PhoneNumberHelper.formatPhoneNumber") {

        context("빈 값 처리") {
            it("빈 문자열이면 빈 문자열 반환") {
                PhoneNumberHelper.formatPhoneNumber("") shouldBe ""
            }

            it("공백만 있으면 빈 문자열 반환") {
                PhoneNumberHelper.formatPhoneNumber("   ") shouldBe ""
            }
        }

        context("이미 포맷된 번호") {
            it("하이픈이 포함되어 있으면 그대로 반환") {
                PhoneNumberHelper.formatPhoneNumber("010-1234-5678") shouldBe "010-1234-5678"
                PhoneNumberHelper.formatPhoneNumber("02-123-4567") shouldBe "02-123-4567"
            }
        }

        context("서울 지역번호 (02)") {
            it("9자리 - 02-123-1234 형식") {
                PhoneNumberHelper.formatPhoneNumber("021231234") shouldBe "02-123-1234"
            }

            it("10자리 - 02-1234-1234 형식") {
                PhoneNumberHelper.formatPhoneNumber("0212341234") shouldBe "02-1234-1234"
            }
        }

        context("지역번호 (031, 032 등)") {
            it("10자리 - 031-123-1234 형식") {
                PhoneNumberHelper.formatPhoneNumber("0311231234") shouldBe "031-123-1234"
            }

            it("11자리 - 031-1234-1234 형식") {
                PhoneNumberHelper.formatPhoneNumber("03112341234") shouldBe "031-1234-1234"
            }
        }

        context("휴대폰 번호") {
            it("11자리 - 010-1234-5678 형식") {
                PhoneNumberHelper.formatPhoneNumber("01012345678") shouldBe "010-1234-5678"
            }

            it("11자리 - 011-1234-5678 형식") {
                PhoneNumberHelper.formatPhoneNumber("01112345678") shouldBe "011-1234-5678"
            }
        }

        context("국가코드 처리") {
            it("+82 국가코드 제거 후 포맷") {
                PhoneNumberHelper.formatPhoneNumber("+821012345678") shouldBe "010-1234-5678"
            }

            it("82 국가코드 제거 후 포맷") {
                PhoneNumberHelper.formatPhoneNumber("821012345678") shouldBe "010-1234-5678"
            }

            it("082 국가코드 제거 후 포맷") {
                PhoneNumberHelper.formatPhoneNumber("0821012345678") shouldBe "010-1234-5678"
            }
        }

        context("공백/특수문자 처리") {
            it("공백 포함된 번호 처리") {
                PhoneNumberHelper.formatPhoneNumber("010 1234 5678") shouldBe "010-1234-5678"
            }

            it("하이픈 없이 숫자만 있는 경우") {
                PhoneNumberHelper.formatPhoneNumber("01012345678") shouldBe "010-1234-5678"
            }
        }

        context("유효하지 않은 형식") {
            it("숫자가 아닌 문자 포함 시 빈 문자열 반환") {
                PhoneNumberHelper.formatPhoneNumber("010abcd5678") shouldBe ""
            }

            it("길이가 맞지 않으면 빈 문자열 반환") {
                PhoneNumberHelper.formatPhoneNumber("0101234") shouldBe ""
                PhoneNumberHelper.formatPhoneNumber("010123456789") shouldBe ""
            }

            it("02로 시작하지 않는 9자리는 빈 문자열 반환") {
                PhoneNumberHelper.formatPhoneNumber("031123123") shouldBe ""
            }
        }
    }
})