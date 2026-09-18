package com.buyeong.umji.api.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.security.jwt")
data class JwtProperties(
    val issuer: String,
    val audience: String,
    val keyId: String,
    val publicKeyPath: String,
    val privateKeyPath: String,
)
