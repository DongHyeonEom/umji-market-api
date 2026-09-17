package com.buyeong.umji.api.persistence.jdbc.converter

import com.buyeong.umji.api.enums.StaffStatusEnum
import com.buyeong.umji.api.enums.StringEnumUtil
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

/**
 * StaffStatusEnum -> String 변환 (DB 저장용).
 */
@WritingConverter
class StaffStatusWritingConverter : Converter<StaffStatusEnum, String> {
    override fun convert(source: StaffStatusEnum): String = source.value
}

/**
 * String -> StaffStatusEnum 변환 (DB 조회용).
 */
@ReadingConverter
class StaffStatusReadingConverter : Converter<String, StaffStatusEnum> {
    override fun convert(source: String): StaffStatusEnum =
        StringEnumUtil.fromValue<StaffStatusEnum>(source)
}