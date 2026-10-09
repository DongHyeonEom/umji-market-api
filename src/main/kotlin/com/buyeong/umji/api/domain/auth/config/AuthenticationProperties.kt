package com.buyeong.umji.api.domain.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.security.authentication")
data class AuthenticationProperties(
    val mode: AuthenticationMode = AuthenticationMode.REQUIRED,
)

enum class AuthenticationMode {
    REQUIRED,
    BYPASS,
}