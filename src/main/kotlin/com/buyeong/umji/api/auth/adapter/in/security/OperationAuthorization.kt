package com.buyeong.umji.api.auth.adapter.`in`.security

import com.buyeong.umji.api.auth.config.AuthenticationMode
import com.buyeong.umji.api.auth.config.AuthenticationProperties
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component("operationAuthorization")
class OperationAuthorization(private val properties: AuthenticationProperties) {
    fun hasPermission(authentication: Authentication?, permission: String): Boolean {
        if (properties.mode == AuthenticationMode.BYPASS) return true
        return authentication?.authorities?.any { it.authority == permission } == true
    }
}