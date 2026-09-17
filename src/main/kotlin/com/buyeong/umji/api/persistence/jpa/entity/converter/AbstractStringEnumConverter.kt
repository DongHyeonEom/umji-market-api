package com.buyeong.umji.api.persistence.jpa.entity.converter

import com.buyeong.umji.api.enums.StringEnum
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * StringEnum을 구현한 Enum 타입의 JPA AttributeConverter 기본 클래스.
 *
 * @param nullable null 허용 여부 (false인 경우 null -> "" 변환)
 * @param fromValue String -> Enum 변환 함수
 */
@Converter
abstract class AbstractStringEnumConverter<T>(
    private val nullable: Boolean = false,
    private val fromValue: (String) -> T,
) : AttributeConverter<T, String> where T : Enum<T>, T : StringEnum {
    override fun convertToDatabaseColumn(attribute: T?): String? =
        when {
            attribute == null && nullable -> null
            attribute == null -> ""
            else -> attribute.value
        }

    override fun convertToEntityAttribute(dbData: String?): T? = dbData?.let(fromValue)
}

/**
 * 빈 문자열을 null로 처리하는 StringEnum Converter.
 *
 * DB에 빈 문자열("")이 저장된 경우 null로 변환합니다.
 */
@Converter
abstract class AbstractBlankStringEnumConverter<T>(
    private val fromValue: (String) -> T?,
) : AttributeConverter<T, String> where T : Enum<T>, T : StringEnum {
    override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): T? =
        if (dbData.isNullOrBlank()) null else fromValue(dbData)
}

/**
 * 잘못된 값에 대해 기본값을 반환하는 StringEnum Converter.
 *
 * DB에 유효하지 않은 값이 저장된 경우 지정된 기본값으로 변환합니다.
 *
 * @param defaultValue 기본값 (null 가능)
 * @param fromValue String -> Enum 변환 함수 (찾지 못하면 null 반환)
 */
@Converter
abstract class AbstractDefaultStringEnumConverter<T>(
    private val defaultValue: T?,
    private val fromValue: (String) -> T?,
) : AttributeConverter<T, String> where T : Enum<T>, T : StringEnum {
    override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): T? =
        if (dbData.isNullOrBlank()) {
            defaultValue
        } else {
            fromValue(dbData) ?: defaultValue
        }
}