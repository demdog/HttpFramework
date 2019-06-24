package com.apigj.android.httpframework.utils

import android.annotation.SuppressLint
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

class AESCipher {
    companion object {
        @Throws(Exception::class)
        fun encrypt(key: String, src: String): String {
            val rawKey = getRawKey(key.toByteArray())
            val result = encrypt(rawKey, src.toByteArray())
            return toHex(result)
        }

        @Throws(Exception::class)
        fun decrypt(key: String, encrypted: String): String {
            val rawKey = getRawKey(key.toByteArray())
            val enc = toByte(encrypted)
            val result = decrypt(rawKey, enc)
            return String(result)
        }

        @SuppressLint("DeletedProvider")
        @Throws(Exception::class)
        fun getRawKey(seed: ByteArray): ByteArray {
            val kgen = KeyGenerator.getInstance("AES")
            // SHA1PRNG ǿ��������㷨
            var sr: SecureRandom? = null
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                sr = SecureRandom.getInstance("SHA1PRNG", "Crypto")
            } else {
                sr = SecureRandom.getInstance("SHA1PRNG")
            }
            //        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
            sr!!.setSeed(seed)
            kgen.init(256, sr) //256 bits or 128 bits,192bits
            val skey = kgen.generateKey()
            return skey.encoded
        }

        @Throws(
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            InvalidKeyException::class,
            BadPaddingException::class,
            IllegalBlockSizeException::class
        )
        fun encrypt(key: ByteArray, src: ByteArray): ByteArray {
            val skeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            return cipher.doFinal(src)
        }

        @Throws(
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            InvalidKeyException::class,
            BadPaddingException::class,
            IllegalBlockSizeException::class
        )
        fun decrypt(key: ByteArray, encrypted: ByteArray): ByteArray {
            val skeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec)
            return cipher.doFinal(encrypted)
        }

        fun toHex(txt: String): String {
            return toHex(txt.toByteArray())
        }

        fun fromHex(hex: String): String {
            return String(toByte(hex))
        }

        fun toByte(hexString: String): ByteArray {
            val len = hexString.length / 2
            val result = ByteArray(len)
            for (i in 0 until len)
                result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).toByte()
            return result
        }

        fun toHex(buf: ByteArray?): String {
            if (buf == null)
                return ""
            val result = StringBuffer(2 * buf.size)
            for (i in buf.indices) {
                appendHex(result, buf[i])
            }
            return result.toString()
        }

        private val HEX = "0123456789ABCDEF"
        private fun appendHex(sb: StringBuffer, b: Byte) {
            sb.append(HEX[b.toInt().shr(4)]).append(HEX[b.toInt()])
        }
    }
}