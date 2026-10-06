package com.buyeong.umji.api.notification.integration.apns

import com.buyeong.umji.api.notification.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.model.NotificationMessage
import com.buyeong.umji.api.notification.model.NotificationProviderResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import org.springframework.http.HttpHeaders

class ApnsHttpPushProvider(
    teamId: String,
    keyId: String,
    privateKeyPath: String,
    topic: String,
    environment: String,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
 ) {
    private val teamId = teamId.also { require(it.isNotBlank()) { "APNs team ID is required." } }
    private val keyId = keyId.also { require(it.isNotBlank()) { "APNs key ID is required." } }
    private val topic = topic.also { require(it.isNotBlank()) { "APNs topic is required." } }
    private val privateKey = loadPrivateKey(privateKeyPath)
    private val baseUrl = when (environment.lowercase()) {
        "production" -> "https://api.push.apple.com"
        "sandbox" -> "https://api.sandbox.push.apple.com"
        else -> throw IllegalArgumentException("APNs environment must be production or sandbox.")
    }
    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(HTTP_TIMEOUT)
        .build()

    @Volatile
    private var cachedProviderToken: String? = null

    @Volatile
    private var providerTokenCreatedAt: Instant = Instant.EPOCH

    fun send(recipient: NotificationDeviceRecipient, message: NotificationMessage): NotificationProviderResult {
        val response = try {
            sendRequest(recipient.token, message, authorizationToken())
        } catch (_: Exception) {
            return NotificationProviderResult.RetryableFailure("APNS_TRANSPORT_ERROR")
        }
        val reason = responseReason(response)
        if (response.statusCode() == 403 && reason == "ExpiredProviderToken") {
            val refreshed = refreshTokenIfAllowed()
                ?: return NotificationProviderResult.RetryableFailure("APNS_PROVIDER_TOKEN_EXPIRED")
            val retry = try {
                sendRequest(recipient.token, message, refreshed)
            } catch (_: Exception) {
                return NotificationProviderResult.RetryableFailure("APNS_TRANSPORT_ERROR")
            }
            return classify(retry)
        }
        return classify(response)
    }

    private fun sendRequest(deviceToken: String, message: NotificationMessage, providerToken: String): HttpResponse<String> {
        val payload = linkedMapOf<String, Any>(
            "aps" to mapOf("alert" to mapOf("title" to message.title, "body" to message.body), "sound" to "default"),
        ).apply { putAll(message.data) }
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/3/device/$deviceToken"))
            .timeout(HTTP_TIMEOUT)
            .header(HttpHeaders.AUTHORIZATION, "bearer $providerToken")
            .header("apns-topic", topic)
            .header("apns-push-type", "alert")
            .header("apns-priority", "10")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

    private fun classify(response: HttpResponse<String>): NotificationProviderResult {
        val reason = responseReason(response)
        return when {
            response.statusCode() in 200..299 -> NotificationProviderResult.Accepted
            response.statusCode() == 410 || reason == "Unregistered" || reason == "BadDeviceToken" ->
                NotificationProviderResult.InvalidToken
            response.statusCode() == 429 || response.statusCode() >= 500 ->
                NotificationProviderResult.RetryableFailure("APNS_HTTP_${response.statusCode()}")
            else -> NotificationProviderResult.PermanentFailure("APNS_${reason.uppercase().replace(UNSAFE_CODE_CHARS, "_").take(60)}")
        }
    }

    private fun responseReason(response: HttpResponse<String>): String =
        runCatching { objectMapper.readTree(response.body())?.path("reason")?.asText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "HTTP_${response.statusCode()}"

    @Synchronized
    private fun authorizationToken(): String {
        val now = clock.instant()
        val existing = cachedProviderToken
        if (existing != null && now.isBefore(providerTokenCreatedAt.plus(TOKEN_REFRESH_AGE))) return existing
        return createProviderToken(now).also {
            cachedProviderToken = it
            providerTokenCreatedAt = now
        }
    }

    @Synchronized
    private fun refreshTokenIfAllowed(): String? {
        val now = clock.instant()
        if (now.isBefore(providerTokenCreatedAt.plus(MIN_TOKEN_REFRESH_INTERVAL))) return null
        return createProviderToken(now).also {
            cachedProviderToken = it
            providerTokenCreatedAt = now
        }
    }

    private fun createProviderToken(now: Instant): String {
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).type(JOSEObjectType.JWT).build(),
            JWTClaimsSet.Builder().issuer(teamId).issueTime(Date.from(now)).build(),
        )
        jwt.sign(ECDSASigner(privateKey))
        return jwt.serialize()
    }

    private fun loadPrivateKey(path: String): ECPrivateKey {
        require(path.isNotBlank()) { "APNs private key path is required." }
        val pem = Files.readString(Path.of(path))
        val encoded = pem
            .replace(PRIVATE_KEY_HEADER, "")
            .replace(PRIVATE_KEY_FOOTER, "")
            .filterNot(Char::isWhitespace)
        val keyBytes = Base64.getDecoder().decode(encoded)
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as ECPrivateKey
    }

    private companion object {
        val HTTP_TIMEOUT: Duration = Duration.ofSeconds(15)
        val TOKEN_REFRESH_AGE: Duration = Duration.ofMinutes(50)
        val MIN_TOKEN_REFRESH_INTERVAL: Duration = Duration.ofMinutes(20)
        const val PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----"
        const val PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----"
        val UNSAFE_CODE_CHARS = Regex("[^A-Z0-9_.-]")
    }
}
