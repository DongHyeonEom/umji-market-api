package com.buyeong.umji.api.domain.account.integration.http

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.business-registration.status")
data class BusinessRegistrationStatusProperties(
    val baseUrl: String = "https://api.odcloud.kr/api",
    val serviceKey: String = "",
)