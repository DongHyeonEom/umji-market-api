package com.buyeong.umji.api.util

/**
 * 한국 전화번호 포맷팅 유틸리티.
 *
 * 지원하는 형식:
 * - 02-123-1234 (서울 9자리)
 * - 02-1234-1234 (서울 10자리)
 * - 031-123-1234 (지역번호 10자리)
 * - 010-1234-1234 (휴대폰 11자리)
 * - 031-1234-1234 (지역번호 11자리)
 */
object PhoneNumberHelper {
    fun normalizeMobilePhoneNumber(phoneNumber: String): String {
        val digits = phoneNumber.filter(Char::isDigit).let {
            when {
                it.startsWith("082") -> "0${it.drop(3)}"
                it.startsWith("82") -> "0${it.drop(2)}"
                else -> it
            }
        }
        require(digits.matches(Regex("010\\d{8}"))) { "유효한 휴대폰 번호가 아닙니다." }
        return digits
    }

    /**
     * 전화번호를 하이픈(-) 포함 형식으로 변환.
     *
     * @param phoneNumber 원본 전화번호 (숫자만 또는 하이픈 포함)
     * @return 포맷된 전화번호. 유효하지 않은 형식이면 빈 문자열 반환
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        if (phoneNumber.contains("-")) return phoneNumber

        val digits =
            phoneNumber
                .replace("-", "")
                .replace(" ", "")
                .replace("+", "")
                .replaceFirst("^0*82".toRegex(), "0")

        if (!digits.all { it.isDigit() }) return ""

        return when (digits.length) {
            9 -> // 02-123-1234
                if (digits.startsWith("02")) {
                    "${digits.take(2)}-${digits.drop(2).take(3)}-${digits.takeLast(4)}"
                } else {
                    ""
                }

            10 ->
                if (digits.startsWith("02")) {
                    // 02-1234-1234
                    "${digits.take(2)}-${digits.drop(2).take(4)}-${digits.takeLast(4)}"
                } else {
                    // 031-123-1234
                    "${digits.take(3)}-${digits.drop(3).take(3)}-${digits.takeLast(4)}"
                }

            11 -> // 010-1234-1234 031-1234-1234
                "${digits.take(3)}-${digits.drop(3).take(4)}-${digits.takeLast(4)}"

            else -> ""
        }
    }
}
