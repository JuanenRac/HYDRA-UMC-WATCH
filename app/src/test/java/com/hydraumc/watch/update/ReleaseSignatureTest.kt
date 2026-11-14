// =============================================================================
// HYDRA-UMC-WATCH - app/src/test/java/com/hydraumc/watch/update/ReleaseSignatureTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
// =============================================================================
package com.hydraumc.watch.update

import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Ed25519 check and the release verdicts. The keys are made here at run time with the JDK, an implementation other
 * than the one under test; the fixed vectors are RFC 8032 test 1 and a signature made by the Python reference of the
 * firmware tools. The pinned key of the releases is never used to sign.
 */
class ReleaseSignatureTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun toHex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun sha256(b: ByteArray) = toHex(MessageDigest.getInstance("SHA-256").digest(b))
    private fun flip(b: ByteArray, at: Int) = b.copyOf().also { it[at] = (it[at].toInt() xor 1).toByte() }

    private class Throwaway(val publicKey: ByteArray, val sign: (ByteArray) -> ByteArray)

    private fun throwaway(): Throwaway {
        val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        // an X.509 Ed25519 key is a 12-byte prefix and the 32 raw bytes
        val encoded = pair.public.encoded
        val raw = encoded.copyOfRange(encoded.size - 32, encoded.size)
        return Throwaway(raw) { message ->
            val signer = Signature.getInstance("Ed25519")
            signer.initSign(pair.private)
            signer.update(message)
            signer.sign()
        }
    }

    private val asset = ByteArray(300) { (it * 11 + 5).toByte() }

    @Test
    fun `RFC 8032 test 1 verifies and a changed bit does not`() {
        val key = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
        val sig = hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b")
        assertTrue(Ed25519.verify(key, ByteArray(0), sig))
        assertFalse(Ed25519.verify(key, byteArrayOf(0), sig))
        assertFalse(Ed25519.verify(key, ByteArray(0), flip(sig, 3)))
    }

    @Test
    fun `a signature made by the Python reference of the firmware tools verifies`() {
        assertTrue(
            Ed25519.verify(
                hex("d28981e878d1dccdd2ca4dde2c2cbf0ea6aad1e1bdd3a08a74e97ccb43b7ce49"),
                hex("48594452412d554d432072656c656173652061737365742062797465730001ff"),
                hex("d950e20ca4434e5545f8990eb9b89cc28c891e2030101b7580234be73adb0727306ad98bbd484e8707b8f9ff3b17d9891036c05eb2327e8ba6950a1b443c8f0a"),
            ),
        )
    }

    @Test
    fun `signatures made by the JDK with keys made here verify and nothing else does`() {
        repeat(3) { n ->
            val key = throwaway()
            val message = ByteArray(40 * (n + 1)) { (it * 7 + 1).toByte() }
            val sig = key.sign(message)
            assertTrue(Ed25519.verify(key.publicKey, message, sig))
            assertFalse(Ed25519.verify(key.publicKey, flip(message, 0), sig))
            assertFalse(Ed25519.verify(key.publicKey, message, flip(sig, 40)))
            assertFalse(Ed25519.verify(throwaway().publicKey, message, sig))
        }
    }

    @Test
    fun `malformed input is false and never an exception`() {
        val key = throwaway()
        val sig = key.sign(byteArrayOf(1, 2, 3))
        assertFalse(Ed25519.verify(key.publicKey.copyOfRange(1, 32), byteArrayOf(1, 2, 3), sig))
        assertFalse(Ed25519.verify(key.publicKey, byteArrayOf(1, 2, 3), sig.copyOfRange(1, 64)))
        assertFalse(Ed25519.verify(ByteArray(32) { -1 }, byteArrayOf(1, 2, 3), sig))
        // a scalar that is not below the group order
        assertFalse(Ed25519.verify(key.publicKey, byteArrayOf(1, 2, 3), sig.copyOfRange(0, 32) + ByteArray(32) { -1 }))
    }

    @Test
    fun `the pinned key is the key of the releases`() {
        assertEquals("1228393cae6cb9fba1d98e300446956de2bc1e9c5b59cc58035a65dbcf4f2331", ReleaseSignature.PINNED_PUBLIC_KEY_HEX)
    }

    @Test
    fun `switch off - the digest rule of the phone app and the signature is not looked at`() {
        val digest = "sha256:" + sha256(asset)
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset))
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset, signatureFile = "not base64 at all"))
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset, rawDigest = digest))
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset, rawDigest = digest.uppercase()))
        assertEquals(ReleaseSignature.Verdict.DIGEST_MISMATCH, ReleaseSignature.verifyDownload(asset, rawDigest = "sha256:" + "0".repeat(64)))
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset, rawDigest = "md5:abcd"))
        assertEquals(ReleaseSignature.Verdict.OK, ReleaseSignature.verifyDownload(asset, rawDigest = "sha256:abcd"))
    }

    @Test
    fun `switch on - every verdict`() {
        val key = throwaway()
        val keyHex = toHex(key.publicKey)
        val signature = Base64.getEncoder().encodeToString(key.sign(asset)) + "\n"

        fun check(bytes: ByteArray = asset, sig: String? = null, digest: String? = null, pub: String = keyHex) =
            ReleaseSignature.verifyDownload(bytes, digest, sig, requireSignature = true, publicKeyHex = pub)

        assertEquals(ReleaseSignature.Verdict.OK, check(sig = signature, digest = "sha256:" + sha256(asset)))
        assertEquals(ReleaseSignature.Verdict.OK, check(sig = signature.trim()))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MISSING, check())
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MISSING, check(sig = ""))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MISSING, check(sig = " \n"))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MALFORMED, check(sig = "!!!!"))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MALFORMED, check(sig = Base64.getEncoder().encodeToString(ByteArray(63) { 1 })))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_MALFORMED, check(sig = Base64.getEncoder().encodeToString(ByteArray(65) { 1 })))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_INVALID, check(bytes = asset + byteArrayOf(0), sig = signature))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_INVALID, check(sig = signature, pub = toHex(throwaway().publicKey)))
        assertEquals(ReleaseSignature.Verdict.SIGNATURE_INVALID, check(sig = signature, pub = "zz"))
        assertEquals(ReleaseSignature.Verdict.DIGEST_MISMATCH, check(sig = signature, digest = "sha256:" + "0".repeat(64)))
        // the real pinned key does not accept a signature made with a throwaway key
        assertEquals(
            ReleaseSignature.Verdict.SIGNATURE_INVALID,
            ReleaseSignature.verifyDownload(asset, signatureFile = signature, requireSignature = true),
        )
    }

    @Test
    fun `a signature file is 64 bytes of base64 with or without a line break`() {
        val sixtyFour = Base64.getEncoder().encodeToString(ByteArray(64) { it.toByte() })
        assertNotNull(ReleaseSignature.decodeSignatureFile(sixtyFour))
        assertNotNull(ReleaseSignature.decodeSignatureFile(sixtyFour + "\n"))
        assertNull(ReleaseSignature.decodeSignatureFile(null))
        assertNull(ReleaseSignature.decodeSignatureFile("   "))
        assertNull(ReleaseSignature.decodeSignatureFile("%%%"))
    }
}
