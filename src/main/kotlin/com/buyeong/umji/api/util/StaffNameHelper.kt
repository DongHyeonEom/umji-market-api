package com.buyeong.umji.api.util

/**
 * 직원 이름 포맷팅 유틸리티.
 *
 * 영문 이름은 "First Last" 순서로, 한글 이름은 "성 이름" 순서로 반환.
 */
object StaffNameHelper {
    private val englishNamePattern = Regex("[a-zA-Z\\s-]+")

    /**
     * 이름을 언어에 맞는 순서로 조합.
     *
     * - 영문 이름: "First Middle Last" 또는 "First Last"
     * - 한글 이름: "성이름" (lastName + firstName)
     *
     * @param firstName 이름 (Given name)
     * @param middleName 중간 이름 (선택적)
     * @param lastName 성 (Family name)
     * @return 조합된 전체 이름
     */
    fun getStaffName(
        firstName: String?,
        middleName: String? = null,
        lastName: String?,
    ): String {
        val first = firstName?.trim() ?: ""
        val last = lastName?.trim() ?: ""

        if (first.isEmpty() && last.isEmpty()) return ""

        val isEnglish = isEnglishName(first, last)

        return if (isEnglish) {
            buildEnglishName(first, middleName?.trim(), last)
        } else {
            "$last$first"
        }
    }

    private fun isEnglishName(
        firstName: String,
        lastName: String,
    ): Boolean =
        (firstName.isEmpty() || englishNamePattern.matches(firstName)) &&
            (lastName.isEmpty() || englishNamePattern.matches(lastName))

    private fun buildEnglishName(
        firstName: String,
        middleName: String?,
        lastName: String,
    ): String =
        if (middleName.isNullOrBlank()) {
            "$firstName $lastName".trim().replace("  ", " ")
        } else {
            "$firstName $middleName $lastName".trim().replace("  ", " ")
        }
}