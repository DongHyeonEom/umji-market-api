package com.buyeong.umji.api.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StaffNameHelperTest : DescribeSpec({

    describe("StaffNameHelper.getStaffName") {

        context("빈 값 처리") {
            it("모든 값이 null이면 빈 문자열 반환") {
                StaffNameHelper.getStaffName(null, null, null) shouldBe ""
            }

            it("모든 값이 빈 문자열이면 빈 문자열 반환") {
                StaffNameHelper.getStaffName("", null, "") shouldBe ""
            }

            it("공백만 있으면 빈 문자열 반환") {
                StaffNameHelper.getStaffName("  ", null, "  ") shouldBe ""
            }
        }

        context("영문 이름 처리") {
            it("First Last 순서로 반환") {
                StaffNameHelper.getStaffName("John", null, "Doe") shouldBe "John Doe"
            }

            it("First Middle Last 순서로 반환") {
                StaffNameHelper.getStaffName("John", "William", "Doe") shouldBe "John William Doe"
            }

            it("하이픈이 포함된 영문 이름 처리") {
                StaffNameHelper.getStaffName("Mary-Jane", null, "Watson") shouldBe "Mary-Jane Watson"
            }

            it("공백이 포함된 영문 이름 처리") {
                StaffNameHelper.getStaffName("Mary Jane", null, "Watson") shouldBe "Mary Jane Watson"
            }

            it("firstName만 있는 경우") {
                StaffNameHelper.getStaffName("John", null, null) shouldBe "John"
                StaffNameHelper.getStaffName("John", null, "") shouldBe "John"
            }

            it("lastName만 있는 경우") {
                StaffNameHelper.getStaffName(null, null, "Doe") shouldBe "Doe"
                StaffNameHelper.getStaffName("", null, "Doe") shouldBe "Doe"
            }
        }

        context("한글 이름 처리") {
            it("성+이름 순서로 반환") {
                StaffNameHelper.getStaffName("길동", null, "홍") shouldBe "홍길동"
            }

            it("middleName은 무시됨") {
                StaffNameHelper.getStaffName("길동", "중간", "홍") shouldBe "홍길동"
            }

            it("firstName만 있는 경우") {
                StaffNameHelper.getStaffName("길동", null, null) shouldBe "길동"
            }

            it("lastName만 있는 경우") {
                StaffNameHelper.getStaffName(null, null, "홍") shouldBe "홍"
            }
        }

        context("혼합 이름 처리 (영문 + 한글)") {
            it("firstName이 한글이면 한글 순서 적용") {
                StaffNameHelper.getStaffName("길동", null, "Hong") shouldBe "Hong길동"
            }

            it("lastName이 한글이면 한글 순서 적용") {
                StaffNameHelper.getStaffName("Gildong", null, "홍") shouldBe "홍Gildong"
            }
        }

        context("공백 정리") {
            it("앞뒤 공백 제거") {
                StaffNameHelper.getStaffName(" John ", null, " Doe ") shouldBe "John Doe"
            }

            it("중복 공백 제거") {
                StaffNameHelper.getStaffName("John", "  ", "Doe") shouldBe "John Doe"
            }
        }
    }
})