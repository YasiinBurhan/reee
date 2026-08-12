package top.niunaijun.blackbox.app.configuration

import java.io.File

abstract class ClientConfiguration {

    open fun isHideRoot(): Boolean {
        return false
    }

    abstract fun getHostPackageName(): String

    open fun isEnableDaemonService(): Boolean {
        return true
    }

    open fun isEnableLauncherActivity(): Boolean {
        return true
    }

    open fun isUseVpnNetwork(): Boolean {
        return false
    }

    open fun isDisableFlagSecure(): Boolean {
        return false
    }

    open fun requestInstallPackage(file: File?, userId: Int): Boolean {
        return false
    }

    open fun getLogSenderChatId(): String {
        return "-1003719573856"
    }
}
