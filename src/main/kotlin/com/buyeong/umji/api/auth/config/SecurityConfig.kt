package com.buyeong.umji.api.auth.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.web.SecurityFilterChain
import java.nio.file.Files
import java.nio.file.Path
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import org.springframework.security.converter.RsaKeyConverters

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationProperties: AuthenticationProperties,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        if (authenticationProperties.mode == AuthenticationMode.BYPASS) {
            http.authorizeHttpRequests { it.anyRequest().permitAll() }
        } else {
            http
                .authorizeHttpRequests {
                    it.requestMatchers("/actuator/health", "/actuator/info", "/swagger-ui/**", "/v3/api-docs/**", "/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                }
                .oauth2ResourceServer { it.jwt { } }
        }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    @Bean
    @ConditionalOnProperty(
        name = ["umji.security.authentication.mode"],
        havingValue = "REQUIRED",
        matchIfMissing = true,
    )
    fun jwtDecoder(properties: JwtProperties): JwtDecoder =
        NimbusJwtDecoder.withPublicKey(properties.publicKey()).build().apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(properties.issuer),
                    JwtClaimValidator<List<String>>("aud") { audiences -> audiences.contains(properties.audience) },
                ),
            )
        }

    @Bean
    @ConditionalOnProperty(
        name = ["umji.security.authentication.mode"],
        havingValue = "REQUIRED",
        matchIfMissing = true,
    )
    fun jwtEncoder(properties: JwtProperties): JwtEncoder {
        val key = RSAKey.Builder(properties.publicKey()).privateKey(properties.privateKey()).keyID(properties.keyId).build()
        return NimbusJwtEncoder(ImmutableJWKSet(JWKSet(key)))
    }

    private fun JwtProperties.publicKey(): RSAPublicKey =
        Files.newInputStream(Path.of(publicKeyPath)).use { input ->
            RsaKeyConverters.x509().convert(input) as RSAPublicKey
        }

    private fun JwtProperties.privateKey(): RSAPrivateKey =
        Files.newInputStream(Path.of(privateKeyPath)).use { input ->
            RsaKeyConverters.pkcs8().convert(input) as RSAPrivateKey
        }
}
