package com.buyeong.umji.api.account.application.port.out

interface BusinessRegistrationStatusPort {
    fun ensureNotClosed(businessRegistrationNumber: String)
}
