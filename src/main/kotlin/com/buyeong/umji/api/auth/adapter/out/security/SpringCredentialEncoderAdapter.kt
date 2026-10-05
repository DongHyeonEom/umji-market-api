package com.buyeong.umji.api.auth.adapter.out.security

import com.buyeong.umji.api.auth.application.port.out.CredentialEncoderPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringCredentialEncoderAdapter(private val passwordEncoder: PasswordEncoder) : CredentialEncoderPort {
    override fun encode(raw: String): String = passwordEncoder.encode(raw)
    override fun matches(raw: String, encoded: String): Boolean = passwordEncoder.matches(raw, encoded)
}