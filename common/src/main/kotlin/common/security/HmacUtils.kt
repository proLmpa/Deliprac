package common.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    private const val ALGORITHM = "HmacSHA256"

    fun sign(secret: String, method: String, path: String, timestampMs: Long, body: ByteArray): String {
        val bodyHash = sha256Hex(body)
        val data = "$method\n$path\n$timestampMs\n$bodyHash"
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM)
        return Mac.getInstance(ALGORITHM).run {
            init(key)
            doFinal(data.toByteArray())
        }.joinToString("") { "%02x".format(it) }
    }

    fun verify(secret: String, method: String, path: String, timestampMs: Long, body: ByteArray, signature: String): Boolean =
        MessageDigest.isEqual(
            sign(secret, method, path, timestampMs, body).toByteArray(),
            signature.toByteArray()
        )

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
