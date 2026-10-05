package com.buyeong.umji.api.auth.adapter.out.security

import com.buyeong.umji.api.auth.application.port.out.TotpPort
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Clock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class Rfc6238TotpAdapter(private val clock: Clock = Clock.systemUTC()) : TotpPort {
    override fun newSecret(): String = ByteArray(20).also(random::nextBytes).let(::base32Encode)

    override fun provisioningUri(accountName: String, secret: String): String {
        val label = URLEncoder.encode("Umji Market:$accountName", StandardCharsets.UTF_8)
        return "otpauth://totp/$label?secret=$secret&issuer=Umji%20Market&algorithm=SHA1&digits=6&period=30"
    }

    override fun verify(secret: String, code: String): Boolean {
        if (!code.matches(Regex("\\d{6}"))) return false
        val key = base32Decode(secret)
        val currentStep = clock.millis() / STEP_MILLIS
        return (-1L..1L).any { offset -> generateCode(key, currentStep + offset) == code }
    }

    private fun generateCode(key: ByteArray, counter: Long): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val digest = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = digest.last().toInt() and 0x0f
        val binary = ((digest[offset].toInt() and 0x7f) shl 24) or
            ((digest[offset + 1].toInt() and 0xff) shl 16) or
            ((digest[offset + 2].toInt() and 0xff) shl 8) or
            (digest[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun base32Encode(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bits = 0
        return buildString {
            bytes.forEach { byte ->
                buffer = (buffer shl 8) or (byte.toInt() and 0xff)
                bits += 8
                while (bits >= 5) {
                    append(alphabet[(buffer shr (bits - 5)) and 31])
                    bits -= 5
                }
            }
            if (bits > 0) append(alphabet[(buffer shl (5 - bits)) and 31])
        }
    }

    private fun base32Decode(value: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bits = 0
        val output = mutableListOf<Byte>()
        value.uppercase().filterNot { it == '=' || it.isWhitespace() }.forEach { char ->
            val digit = alphabet.indexOf(char)
            require(digit >= 0) { "Invalid Base32 secret" }
            buffer = (buffer shl 5) or digit
            bits += 5
            if (bits >= 8) {
                output += ((buffer shr (bits - 8)) and 0xff).toByte()
                bits -= 8
            }
        }
        return output.toByteArray()
    }

    private companion object {
        const val STEP_MILLIS = 30_000L
        val random = SecureRandom()
    }
}