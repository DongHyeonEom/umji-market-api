package com.buyeong.umji.api.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class SchoolTypeEnum(
    @JsonValue
    override val value: String,
) : StringEnum {
    CDI("CDI"),
    THE_OPEN("THEOPEN"),
    APRIL("APRIL"),
    BOUNCY("Bouncy"),
    IGARTEN("i-GARTEN"),
    MT("MT"),
    NOISY("NOISY"),
    CODING("CODING"),
    MG("MG"),
    EDGE("EDGE"),
    EI("EI"),
    CREVILL("CREVILL"),
    LMF("LMF"),
    UMJI_MARKET("UmjiMarket"),
    PREP("PREP"),
    ASP("ASP"),
    ALUMNI("ALUMNI"),
    CLUE("CLUE"),
    CDA("CDA"),
    PAGE("Page"),
    VON_CDI("V.ON-CDI"),
    VON_APRIL("V.ON-April"),
    CIS("CIS"),
}