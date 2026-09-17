package com.buyeong.umji.api.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class StaffStatusEnum(
    @JsonValue
    override val value: String,
) : StringEnum {
    PROMOTION("Promotion"),
    APPLICANT("Applicant"),
    INTERVIEWEE("Interviewee"),
    TRAINEE("Trainee"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DISMISSED("Dismissed"),
    VACATION("Vacation"),
    REJECTED("Rejected"),
    SICK("Sick"),
    QUIT("Quit"),
    REAPPLICANT("Reapplicant"),
    ADMIN("Admin"),
    UNKNOWN(""),
}