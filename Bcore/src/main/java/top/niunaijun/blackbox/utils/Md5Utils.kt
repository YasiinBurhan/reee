package top.niunaijun.blackbox.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object Md5Utils {

    private val HEX_DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    @JvmStatic
    fun md5(input: String?): String? {
        if (input == null) return null
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val inputByteArray = input.toByteArray(Charsets.UTF_8)
            messageDigest.update(inputByteArray)
            byteArrayToHex(messageDigest.digest())
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun md5(file: File?): String? {
        if (file == null || !file.isFile) return null
        return try {
            FileInputStream(file).use { input ->
                md5(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun md5(input: InputStream?): String? {
        if (input == null) return null
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                messageDigest.update(buffer, 0, read)
            }
            byteArrayToHex(messageDigest.digest())
        } catch (e: Exception) {
            null
        }
    }

    private fun byteArrayToHex(byteArray: ByteArray): String {
        val resultCharArray = CharArray(byteArray.size * 2)
        var index = 0
        for (b in byteArray) {
            val bInt = b.toInt()
            resultCharArray[index++] = HEX_DIGITS[bInt ushr 4 and 0xf]
            resultCharArray[index++] = HEX_DIGITS[bInt and 0xf]
        }
        return String(resultCharArray)
    }
}
