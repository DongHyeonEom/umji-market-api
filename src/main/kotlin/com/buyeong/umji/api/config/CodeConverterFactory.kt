package com.buyeong.umji.api.config

import com.buyeong.umji.api.enums.StringEnum
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory

/**
 * StringEnum을 구현한 Enum 타입의 요청 파라미터 변환을 위한 ConverterFactory.
 *
 * Path Variable, Query Parameter에서 String -> Enum 변환 시 사용됩니다.
 * Enum의 value 속성과 대소문자 무관하게 매칭합니다.
 *
 * 사용 예시:
 * ```kotlin
 * // Enum 정의
 * enum class StaffStatus(override val value: String) : StringEnum {
 *     ACTIVE("A"), INACTIVE("I")
 * }
 *
 * // Controller에서 사용
 * @GetMapping("/staffs")
 * fun getStaffs(@RequestParam status: StaffStatus): List<Staff>
 *
 * // 요청: GET /staffs?status=A -> StaffStatus.ACTIVE로 변환
 * ```
 */
class CodeConverterFactory : ConverterFactory<String, StringEnum> {
    override fun <T : StringEnum> getConverter(targetType: Class<T>): Converter<String, T> =
        StringToCodeConverter(targetType)

    private class StringToCodeConverter<T : StringEnum>(
        private val codeType: Class<T>,
    ) : Converter<String, T> {
        override fun convert(source: String): T {
            val constants = codeType.enumConstants
                ?: throw IllegalArgumentException("Enum constants not found for ${codeType.name}")

            for (constant in constants) {
                if (constant.value.equals(source, ignoreCase = true)) {
                    return constant
                }
            }
            throw IllegalArgumentException("No matching enum constant for value: $source in ${codeType.name}")
        }
    }
}