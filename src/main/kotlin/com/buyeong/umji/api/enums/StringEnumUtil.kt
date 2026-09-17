package com.buyeong.umji.api.enums

object StringEnumUtil {
    inline fun <reified T> fromValue(value: String): T where T : Enum<T>, T : StringEnum =
        enumValues<T>().firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Unknown value[$value] for enum type ${T::class.simpleName}")
}