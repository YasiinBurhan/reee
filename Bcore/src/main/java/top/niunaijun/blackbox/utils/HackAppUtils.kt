package top.niunaijun.blackbox.utils

object HackAppUtils {
    @JvmStatic
    fun enableQQLogOutput(packageName: String?, classLoader: ClassLoader) {
        if ("com.tencent.mobileqq" == packageName) {
            try {
                Reflector.on("com.tencent.qphone.base.util.QLog", true, classLoader)
                    .field("UIN_REPORTLOG_LEVEL")
                    .set(100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
