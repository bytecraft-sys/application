package com.yourapp.migration

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlinx.serialization.json.Json

class MigrationCrypto @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val secureRandom = SecureRandom()

    fun encryptZip(
        uid: String,
        bundleUuid: String,
        zipFile: File,
        encryptedFile: File,
    ): File {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val key = deriveAesKey(uid = uid, bundleUuid = bundleUuid)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        encryptedFile.parentFile?.mkdirs()
        FileOutputStream(encryptedFile).use { output ->
            val header = MigrationBundleHeader(
                bundleUuid = bundleUuid,
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            )
            output.write(json.encodeToString(MigrationBundleHeader.serializer(), header).toByteArray())
            output.write('\n'.code)
            FileInputStream(zipFile).use { input ->
                CipherOutputStream(output, cipher).use { cipherOutput ->
                    input.copyTo(cipherOutput)
                }
            }
        }

        return encryptedFile
    }

    fun decryptZip(
        uid: String,
        encryptedFile: File,
        destinationZipFile: File,
    ): Pair<String, File> {
        destinationZipFile.parentFile?.mkdirs()
        FileInputStream(encryptedFile).use { input ->
            val header = json.decodeFromString(
                MigrationBundleHeader.serializer(),
                input.readHeaderLine(),
            )
            val iv = Base64.decode(header.ivBase64, Base64.NO_WRAP)
            val key = deriveAesKey(uid = uid, bundleUuid = header.bundleUuid)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

            FileOutputStream(destinationZipFile).use { output ->
                CipherInputStream(input, cipher).use { cipherInput ->
                    cipherInput.copyTo(output)
                }
            }
            return header.bundleUuid to destinationZipFile
        }
    }

    private fun deriveAesKey(
        uid: String,
        bundleUuid: String,
    ): SecretKeySpec {
        val keyBytes = hkdfSha256(
            ikm = uid.toByteArray(Charsets.UTF_8),
            salt = bundleUuid.toByteArray(Charsets.UTF_8),
            info = HKDF_INFO.toByteArray(Charsets.UTF_8),
            outputLength = AES_256_KEY_BYTES,
        )
        return SecretKeySpec(keyBytes, AES_KEY_ALGORITHM)
    }

    private fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLength: Int,
    ): ByteArray {
        val extractMac = Mac.getInstance(HMAC_SHA256)
        extractMac.init(SecretKeySpec(salt, HMAC_SHA256))
        val prk = extractMac.doFinal(ikm)

        val output = ByteArrayOutputStream()
        var previous = ByteArray(0)
        var counter = 1

        while (output.size() < outputLength) {
            val expandMac = Mac.getInstance(HMAC_SHA256)
            expandMac.init(SecretKeySpec(prk, HMAC_SHA256))
            expandMac.update(previous)
            expandMac.update(info)
            expandMac.update(counter.toByte())
            previous = expandMac.doFinal()
            output.write(previous)
            counter++
        }

        return output.toByteArray().copyOf(outputLength)
    }

    private fun FileInputStream.readHeaderLine(): String {
        val output = ByteArrayOutputStream()
        while (true) {
            val nextByte = read()
            if (nextByte == -1) {
                throw MigrationRejectedException("Encrypted migration bundle is missing a header.")
            }
            if (nextByte == '\n'.code) {
                return output.toString(Charsets.UTF_8.name())
            }
            output.write(nextByte)
        }
    }

    private companion object {
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_ALGORITHM = "AES"
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val HKDF_INFO = "migration-v1"
        private const val AES_256_KEY_BYTES = 32
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
