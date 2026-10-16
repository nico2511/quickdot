package dev.octoshrimpy.quik.billing

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Local check of a Play in-app purchase.
 *
 * [originalJson] is [com.android.billingclient.api.Purchase.getOriginalJson]
 * and [signatureBase64] is [com.android.billingclient.api.Purchase.getSignature].
 * [licenseKeyBase64] is the Play Console license public key (X.509 SubjectPublicKeyInfo,
 * Base64). The signature algorithm is SHA1withRSA, which is what Play uses.
 *
 * A blank key skips the check only when [allowMissingKey] is true (debug).
 * Release builds fail closed: no key, or a signature that does not verify,
 * does not unlock.
 */
fun purchaseMayUnlock(
    originalJson: String?,
    signatureBase64: String?,
    licenseKeyBase64: String?,
    allowMissingKey: Boolean
): Boolean {
    val key = licenseKeyBase64?.trim().orEmpty()
    if (key.isEmpty()) return allowMissingKey
    return verifyPlayPurchase(originalJson.orEmpty(), signatureBase64.orEmpty(), key)
}

fun verifyPlayPurchase(
    originalJson: String,
    signatureBase64: String,
    licenseKeyBase64: String
): Boolean {
    if (originalJson.isEmpty() || signatureBase64.isBlank() || licenseKeyBase64.isBlank()) {
        return false
    }
    return try {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(decodeBase64(licenseKeyBase64))
        )
        val verifier = Signature.getInstance("SHA1withRSA")
        verifier.initVerify(publicKey)
        verifier.update(originalJson.toByteArray(Charsets.UTF_8))
        verifier.verify(decodeBase64(signatureBase64))
    } catch (e: Exception) {
        false
    }
}

/** Standard Base64, plus the URL-safe alphabet. Whitespace is ignored. */
internal fun decodeBase64(value: String): ByteArray {
    val cleaned = StringBuilder(value.length)
    for (ch in value) {
        when (ch) {
            ' ', '\n', '\r', '\t' -> Unit
            '-' -> cleaned.append('+')
            '_' -> cleaned.append('/')
            else -> cleaned.append(ch)
        }
    }
    val remainder = cleaned.length % 4
    if (remainder == 1) throw IllegalArgumentException("bad base64 length")
    if (remainder > 0) {
        repeat(4 - remainder) { cleaned.append('=') }
    }
    val out = ByteArray(cleaned.length / 4 * 3)
    var written = 0
    var index = 0
    while (index < cleaned.length) {
        val c0 = cleaned[index]
        val c1 = cleaned[index + 1]
        val c2 = cleaned[index + 2]
        val c3 = cleaned[index + 3]
        if (c0 == '=' || c1 == '=') throw IllegalArgumentException("bad base64 padding")
        val b0 = base64Value(c0)
        val b1 = base64Value(c1)
        out[written++] = ((b0 shl 2) or (b1 shr 4)).toByte()
        if (c2 != '=') {
            val b2 = base64Value(c2)
            out[written++] = (((b1 and 0x0F) shl 4) or (b2 shr 2)).toByte()
            if (c3 != '=') {
                val b3 = base64Value(c3)
                out[written++] = (((b2 and 0x03) shl 6) or b3).toByte()
            }
        } else if (c3 != '=') {
            throw IllegalArgumentException("bad base64 padding")
        }
        index += 4
    }
    return out.copyOf(written)
}

private fun base64Value(ch: Char): Int {
    val value = when (ch) {
        in 'A'..'Z' -> ch.code - 'A'.code
        in 'a'..'z' -> ch.code - 'a'.code + 26
        in '0'..'9' -> ch.code - '0'.code + 52
        '+' -> 62
        '/' -> 63
        else -> -1
    }
    if (value < 0) throw IllegalArgumentException("bad base64 character")
    return value
}
