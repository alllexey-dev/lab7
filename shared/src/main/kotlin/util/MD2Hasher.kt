package util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MD2Hasher {
    companion object {
        fun getMD2Hash(input: String): String {

            val md2 = MessageDigest.getInstance("MD2") ?: throw NoSuchAlgorithmException()

            val hashBytes = md2.digest(input.toByteArray(Charsets.UTF_8))

            return hashBytes.joinToString("") { String.format("%02x", it) }
        }
    }
}