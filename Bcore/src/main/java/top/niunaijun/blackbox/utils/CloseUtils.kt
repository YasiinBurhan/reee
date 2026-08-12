package top.niunaijun.blackbox.utils

import java.io.Closeable
import java.io.IOException

object CloseUtils {

    @JvmStatic
    fun close(vararg closeables: Closeable?) {
        for (closeable in closeables) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }
}
