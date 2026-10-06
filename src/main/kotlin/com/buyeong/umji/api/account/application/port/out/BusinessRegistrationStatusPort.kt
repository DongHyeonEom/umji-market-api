package com.buyeong.umji.api.account.application.port.out

interface BusinessRegistrationStatusPort {
    fun ensureNotClosed(businessRegistrationNumber: String)

    fun lookup(businessRegistrationNumber: String): BusinessRegistrationStatus
}

enum class BusinessRegistrationStatus {
    ACTIVE,
    TEMPORARILY_CLOSED,
    CLOSED,
    UNKNOWN,
}
