package dev.octoshrimpy.quik.billing

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PlayLicenseTest {

    @Test
    fun signedPurchaseVerifies() {
        val json = """{"orderId":"GPA.1","productId":"quickdot_pro","purchaseState":0}"""
        val keys = rsaKey()
        val signature = sign(json, keys)
        val license = licenseKey(keys)
        assertTrue(verifyPlayPurchase(json, signature, license))
        assertTrue(purchaseMayUnlock(json, signature, license, allowMissingKey = false))
    }

    @Test
    fun tamperedJsonDoesNotVerify() {
        val json = """{"orderId":"GPA.1","productId":"quickdot_pro"}"""
        val keys = rsaKey()
        assertFalse(verifyPlayPurchase(json + " ", sign(json, keys), licenseKey(keys)))
    }

    @Test
    fun wrongLicenseKeyDoesNotVerify() {
        val json = """{"productId":"quickdot_pro"}"""
        val signedBy = rsaKey()
        val other = rsaKey()
        assertFalse(verifyPlayPurchase(json, sign(json, signedBy), licenseKey(other)))
    }

    @Test
    fun blankSignatureOrJsonDoesNotVerify() {
        val keys = rsaKey()
        val json = """{"productId":"quickdot_pro"}"""
        val license = licenseKey(keys)
        assertFalse(verifyPlayPurchase(json, "", license))
        assertFalse(verifyPlayPurchase("", sign(json, keys), license))
        assertFalse(verifyPlayPurchase(json, "not base64!!!", license))
    }

    @Test
    fun missingKeySkipsOnlyDebugAndFailsClosedInRelease() {
        val json = """{"productId":"quickdot_pro"}"""
        val keys = rsaKey()
        val signature = sign(json, keys)
        assertTrue(purchaseMayUnlock(json, signature, "", allowMissingKey = true))
        assertTrue(purchaseMayUnlock(json, "", "  ", allowMissingKey = true))
        assertFalse(purchaseMayUnlock(json, signature, "", allowMissingKey = false))
        assertFalse(purchaseMayUnlock(json, signature, null, allowMissingKey = false))
    }

    @Test
    fun presentKeyStillRejectsABadSignatureInDebug() {
        val json = """{"productId":"quickdot_pro"}"""
        val keys = rsaKey()
        assertFalse(
            purchaseMayUnlock(json, "AAAA", licenseKey(keys), allowMissingKey = true)
        )
    }

    @Test
    fun licenseKeyMayContainWhitespace() {
        val json = """{"productId":"quickdot_pro"}"""
        val keys = rsaKey()
        val license = licenseKey(keys)
        val wrapped = license.chunked(32).joinToString("\n")
        assertTrue(verifyPlayPurchase(json, sign(json, keys), wrapped))
    }

    @Test
    fun decoderMatchesThePlatformEncoder() {
        val raw = ByteArray(64) { index -> (index * 17 - 3).toByte() }
        val encoded = Base64.getEncoder().encodeToString(raw)
        assertArrayEquals(raw, decodeBase64(encoded))
        assertArrayEquals(raw, decodeBase64(encoded.chunked(8).joinToString("\n")))
        val url = Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
        assertArrayEquals(raw, decodeBase64(url))
    }

    private fun rsaKey(): java.security.KeyPair {
        return KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }

    private fun sign(json: String, keys: java.security.KeyPair): String {
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initSign(keys.private)
        signer.update(json.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    private fun licenseKey(keys: java.security.KeyPair): String {
        return Base64.getEncoder().encodeToString(keys.public.encoded)
    }
}
