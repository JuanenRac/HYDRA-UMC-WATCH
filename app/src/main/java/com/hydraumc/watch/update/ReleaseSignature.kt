// =============================================================================
// HYDRA-UMC-WATCH - app/src/main/java/com/hydraumc/watch/update/ReleaseSignature.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D) <electrohobby3d@gmail.com>
// GPL-3.0 - see LICENSE
//
// What the app checks about a downloaded release asset: the digest rule of the phone app (the `sha256:` digest of the
// release feed must match the bytes; a feed without one cannot be compared) and, only when the switch is on (off by
// default, `updates.require_release_signature` in watch.cfg), the Ed25519 signature of the release (`<asset>.sig`,
// base64, over the exact bytes) against the pinned public key of the releases. Pure Kotlin: BigInteger and SHA-512 of
// the platform, no network and no private key.
// =============================================================================
package com.hydraumc.watch.update

import com.hydraumc.common.ReleaseMetadataParser
import java.math.BigInteger
import java.security.MessageDigest

object ReleaseSignature {
    /** The public key of the releases (docs/RELEASE_SIGNING.md of HYDRA-UMC): the one every program of the ecosystem pins. */
    const val PINNED_PUBLIC_KEY_HEX = "1228393cae6cb9fba1d98e300446956de2bc1e9c5b59cc58035a65dbcf4f2331"

    enum class Verdict {
        /** Accepted under the rules in force. */
        OK,

        /** The feed published a digest and the bytes do not match it. */
        DIGEST_MISMATCH,

        /** The switch is on and the release has no `.sig`. */
        SIGNATURE_MISSING,

        /** The switch is on and the `.sig` is not 64 bytes of base64. */
        SIGNATURE_MALFORMED,

        /** The switch is on and the signature does not verify against the pinned key. */
        SIGNATURE_INVALID,
    }

    /** The 64 bytes of a `.sig` file (base64, with or without a final line break), or null when it is not that. */
    fun decodeSignatureFile(text: String?): ByteArray? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val bytes = try {
            java.util.Base64.getDecoder().decode(trimmed)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return bytes.takeIf { it.size == 64 }
    }

    /**
     * Checks the [bytes] of a downloaded asset. [rawDigest] is the `digest` field of the asset in the release feed and
     * [signatureFile] the text of its `.sig`. The signature counts only when [requireSignature] is true.
     */
    fun verifyDownload(
        bytes: ByteArray,
        rawDigest: String? = null,
        signatureFile: String? = null,
        requireSignature: Boolean = false,
        publicKeyHex: String = PINNED_PUBLIC_KEY_HEX,
    ): Verdict {
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val expected = rawDigest?.let { ReleaseMetadataParser.parseDigest(it) }
        if (!ReleaseMetadataParser.digestMatches(expected, actual)) return Verdict.DIGEST_MISMATCH
        if (!requireSignature) return Verdict.OK
        if (signatureFile.isNullOrBlank()) return Verdict.SIGNATURE_MISSING
        val signature = decodeSignatureFile(signatureFile) ?: return Verdict.SIGNATURE_MALFORMED
        val key = hexBytes(publicKeyHex) ?: return Verdict.SIGNATURE_INVALID
        return if (Ed25519.verify(key, bytes, signature)) Verdict.OK else Verdict.SIGNATURE_INVALID
    }

    private fun hexBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = (hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: return null).toByte()
        }
        return out
    }
}

/** Ed25519 verification (RFC 8032) with BigInteger. Signing is not here: the app holds no private key. */
internal object Ed25519 {
    private val one = BigInteger.ONE
    private val zero = BigInteger.ZERO
    private val two = BigInteger.valueOf(2)
    private val p = one.shiftLeft(255).subtract(BigInteger.valueOf(19))
    private val q = one.shiftLeft(252).add(BigInteger("27742317777372353535851937790883648493"))
    private val d = BigInteger.valueOf(-121665).multiply(BigInteger.valueOf(121666).modInverse(p)).mod(p)
    private val sqrtMinusOne = two.modPow(p.subtract(one).divide(BigInteger.valueOf(4)), p)

    private class Point(val x: BigInteger, val y: BigInteger, val z: BigInteger, val t: BigInteger)

    private val neutral = Point(zero, one, one, zero)

    private fun add(a: Point, b: Point): Point {
        val aa = a.y.subtract(a.x).multiply(b.y.subtract(b.x)).mod(p)
        val bb = a.y.add(a.x).multiply(b.y.add(b.x)).mod(p)
        val cc = two.multiply(a.t).multiply(b.t).multiply(d).mod(p)
        val dd = two.multiply(a.z).multiply(b.z).mod(p)
        val e = bb.subtract(aa)
        val f = dd.subtract(cc)
        val g = dd.add(cc)
        val h = bb.add(aa)
        return Point(e.multiply(f).mod(p), g.multiply(h).mod(p), f.multiply(g).mod(p), e.multiply(h).mod(p))
    }

    private fun multiply(scalar: BigInteger, point: Point): Point {
        var result = neutral
        var addend = point
        for (bit in 0 until scalar.bitLength()) {
            if (scalar.testBit(bit)) result = add(result, addend)
            addend = add(addend, addend)
        }
        return result
    }

    private fun same(a: Point, b: Point): Boolean =
        a.x.multiply(b.z).subtract(b.x.multiply(a.z)).mod(p).signum() == 0 &&
            a.y.multiply(b.z).subtract(b.y.multiply(a.z)).mod(p).signum() == 0

    private fun fromLittleEndian(bytes: ByteArray): BigInteger = BigInteger(1, bytes.reversedArray())

    private fun recoverX(y: BigInteger, sign: Int): BigInteger? {
        if (y >= p) return null
        val x2 = y.multiply(y).subtract(one).multiply(d.multiply(y).multiply(y).add(one).modInverse(p)).mod(p)
        if (x2.signum() == 0) return if (sign == 1) null else zero
        var x = x2.modPow(p.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), p)
        if (x.multiply(x).subtract(x2).mod(p).signum() != 0) x = x.multiply(sqrtMinusOne).mod(p)
        if (x.multiply(x).subtract(x2).mod(p).signum() != 0) return null
        if ((if (x.testBit(0)) 1 else 0) != sign) x = p.subtract(x)
        return x
    }

    private fun decompress(bytes: ByteArray): Point? {
        if (bytes.size != 32) return null
        val value = fromLittleEndian(bytes)
        val sign = if (value.testBit(255)) 1 else 0
        val y = value.clearBit(255)
        val x = recoverX(y, sign) ?: return null
        return Point(x, y, one, x.multiply(y).mod(p))
    }

    private val base: Point = run {
        val y = BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(p)).mod(p)
        val x = recoverX(y, 0)!!
        Point(x, y, one, x.multiply(y).mod(p))
    }

    private fun sha512(vararg parts: ByteArray): BigInteger {
        val digest = MessageDigest.getInstance("SHA-512")
        for (part in parts) digest.update(part)
        return fromLittleEndian(digest.digest())
    }

    /** Whether [signature] (64 bytes) is a valid signature of [message] under [publicKey] (32 bytes); malformed input is false. */
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false
        val a = decompress(publicKey) ?: return false
        val rBytes = signature.copyOfRange(0, 32)
        val r = decompress(rBytes) ?: return false
        val s = fromLittleEndian(signature.copyOfRange(32, 64))
        if (s >= q) return false
        val k = sha512(rBytes, publicKey, message).mod(q)
        return same(multiply(s, base), add(r, multiply(k, a)))
    }
}
