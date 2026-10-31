package com.example.androidaiagent

import java.security.MessageDigest
import java.security.SecureRandom

object RemotePairing {

    private val random = SecureRandom()

    private var codeHash: ByteArray? = null
    private var expiresAt: Long = 0L

    @Synchronized
    fun generateCode(): String {
        val code = random.nextInt(1_000_000)
            .toString()
            .padStart(6, '0')

        codeHash = MessageDigest.getInstance("SHA-256")
            .digest(code.toByteArray(Charsets.UTF_8))

        expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L

        return code
    }

    @Synchronized
    fun verifyCode(input: String): Boolean {
        if (System.currentTimeMillis() > expiresAt) {
            clear()
            return false
        }

        if (!input.matches(Regex("[0-9]{6}"))) {
            return false
        }

        val expected = codeHash ?: return false

        val supplied = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))

        val valid = MessageDigest.isEqual(expected, supplied)

        if (valid) clear()

        return valid
    }

    @Synchronized
    fun clear() {
        codeHash?.fill(0)
        codeHash = null
        expiresAt = 0L
    }
}
