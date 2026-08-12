package top.niunaijun.blackbox.util

import android.os.Build
import android.text.TextUtils
import top.niunaijun.blackbox.utils.Slog

object XiaomiDeviceDetector {
    private const val TAG = "XiaomiDeviceDetector"

    private val XIAOMI_MANUFACTURERS = arrayOf(
        "Xiaomi", "Redmi", "POCO", "Black Shark", "Mi"
    )

    private val MIUI_VERSION_PATTERNS = arrayOf(
        "MIUI", "HyperOS", "MIUI Global", "MIUI China"
    )

    private val XIAOMI_MODELS = arrayOf(
        "Mi ", "Redmi ", "POCO ", "Black Shark ", "Xiaomi "
    )

    @Volatile
    private var sIsXiaomiDevice = false

    @Volatile
    private var sMiuiVersion: String? = null

    @Volatile
    private var sDeviceModel: String? = null

    @Volatile
    private var sAndroidVersion = 0

    @JvmStatic
    fun isXiaomiDevice(): Boolean {
        if (sIsXiaomiDevice) {
            return true
        }

        try {
            val manufacturer = Build.MANUFACTURER
            if (isXiaomiManufacturer(manufacturer)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by manufacturer: $manufacturer")
                return true
            }

            val brand = Build.BRAND
            if (isXiaomiManufacturer(brand)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by brand: $brand")
                return true
            }

            val model = Build.MODEL
            if (isXiaomiModel(model)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by model: $model")
                return true
            }

            val product = Build.PRODUCT
            if (isXiaomiModel(product)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by product: $product")
                return true
            }

            val device = Build.DEVICE
            if (isXiaomiModel(device)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by device: $device")
                return true
            }

            val fingerprint = Build.FINGERPRINT
            if (isXiaomiFingerprint(fingerprint)) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by fingerprint: $fingerprint")
                return true
            }

            if (checkXiaomiSystemProperties()) {
                sIsXiaomiDevice = true
                Slog.d(TAG, "Detected Xiaomi device by system properties")
                return true
            }
        } catch (e: Exception) {
            Slog.w(TAG, "Error detecting Xiaomi device: ${e.message}")
        }

        sIsXiaomiDevice = false
        return false
    }

    @JvmStatic
    fun getMiuiVersion(): String? {
        sMiuiVersion?.let { return it }

        if (!isXiaomiDevice()) {
            return null
        }

        try {
            var prop = getSystemProperty("ro.miui.ui.version.name")
            if (!TextUtils.isEmpty(prop)) {
                sMiuiVersion = prop
                Slog.d(TAG, "Detected MIUI version: $prop")
                return prop
            }

            prop = getSystemProperty("ro.miui.version.code")
            if (!TextUtils.isEmpty(prop)) {
                sMiuiVersion = prop
                Slog.d(TAG, "Detected MIUI version code: $prop")
                return prop
            }

            val buildDesc = Build.DISPLAY
            if (!TextUtils.isEmpty(buildDesc) && containsMiuiVersion(buildDesc)) {
                val version = extractMiuiVersion(buildDesc)
                sMiuiVersion = version
                Slog.d(TAG, "Detected MIUI version from build: $version")
                return version
            }
        } catch (e: Exception) {
            Slog.w(TAG, "Error getting MIUI version: ${e.message}")
        }

        sMiuiVersion = "Unknown"
        return sMiuiVersion
    }

    @JvmStatic
    fun getDeviceModel(): String? {
        sDeviceModel?.let { return it }
        val model = Build.MODEL
        sDeviceModel = model
        return model
    }

    @JvmStatic
    fun getAndroidVersion(): Int {
        if (sAndroidVersion > 0) {
            return sAndroidVersion
        }
        val sdk = Build.VERSION.SDK_INT
        sAndroidVersion = sdk
        return sdk
    }

    @JvmStatic
    fun isMiui12OrHigher(): Boolean {
        if (!isXiaomiDevice()) return false
        val miuiVersion = getMiuiVersion()
        if (TextUtils.isEmpty(miuiVersion) || "Unknown" == miuiVersion) return false
        return try {
            val versionNumber = extractVersionNumber(miuiVersion)
            if (!TextUtils.isEmpty(versionNumber)) {
                versionNumber!!.toInt() >= 12
            } else false
        } catch (e: Exception) {
            Slog.w(TAG, "Error checking MIUI version: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun isMiui13OrHigher(): Boolean {
        if (!isXiaomiDevice()) return false
        val miuiVersion = getMiuiVersion()
        if (TextUtils.isEmpty(miuiVersion) || "Unknown" == miuiVersion) return false
        return try {
            val versionNumber = extractVersionNumber(miuiVersion)
            if (!TextUtils.isEmpty(versionNumber)) {
                versionNumber!!.toInt() >= 13
            } else false
        } catch (e: Exception) {
            Slog.w(TAG, "Error checking MIUI version: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun isHyperOS(): Boolean {
        if (!isXiaomiDevice()) return false
        val miuiVersion = getMiuiVersion()
        if (TextUtils.isEmpty(miuiVersion) || "Unknown" == miuiVersion) return false
        return miuiVersion!!.contains("HyperOS") || miuiVersion.contains("OS1.")
    }

    @JvmStatic
    fun getDeviceInfo(): String {
        val info = StringBuilder()
        info.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
        info.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        info.append("Xiaomi Device: ").append(isXiaomiDevice()).append("\n")

        if (isXiaomiDevice()) {
            info.append("MIUI Version: ").append(getMiuiVersion()).append("\n")
            info.append("MIUI 12+: ").append(isMiui12OrHigher()).append("\n")
            info.append("MIUI 13+: ").append(isMiui13OrHigher()).append("\n")
            info.append("HyperOS: ").append(isHyperOS()).append("\n")
        }

        return info.toString()
    }

    private fun isXiaomiManufacturer(manufacturer: String?): Boolean {
        if (TextUtils.isEmpty(manufacturer)) return false
        val lowerManufacturer = manufacturer!!.lowercase()
        for (xiaomiManufacturer in XIAOMI_MANUFACTURERS) {
            if (lowerManufacturer.contains(xiaomiManufacturer.lowercase())) {
                return true
            }
        }
        return false
    }

    private fun isXiaomiModel(model: String?): Boolean {
        if (TextUtils.isEmpty(model)) return false
        val lowerModel = model!!.lowercase()
        for (xiaomiModel in XIAOMI_MODELS) {
            if (lowerModel.contains(xiaomiModel.lowercase())) {
                return true
            }
        }
        return false
    }

    private fun isXiaomiFingerprint(fingerprint: String?): Boolean {
        if (TextUtils.isEmpty(fingerprint)) return false
        val lowerFingerprint = fingerprint!!.lowercase()
        return lowerFingerprint.contains("xiaomi") ||
                lowerFingerprint.contains("redmi") ||
                lowerFingerprint.contains("poco") ||
                lowerFingerprint.contains("blackshark") ||
                lowerFingerprint.contains("mi ")
    }

    private fun checkXiaomiSystemProperties(): Boolean {
        try {
            var miuiProperty = getSystemProperty("ro.miui.ui.version.name")
            if (!TextUtils.isEmpty(miuiProperty)) return true

            miuiProperty = getSystemProperty("ro.miui.version.code")
            if (!TextUtils.isEmpty(miuiProperty)) return true

            miuiProperty = getSystemProperty("ro.miui.build.version")
            if (!TextUtils.isEmpty(miuiProperty)) return true
        } catch (e: Exception) {
            Slog.w(TAG, "Error checking Xiaomi system properties: ${e.message}")
        }
        return false
    }

    private fun containsMiuiVersion(text: String?): Boolean {
        if (TextUtils.isEmpty(text)) return false
        val lowerText = text!!.lowercase()
        for (pattern in MIUI_VERSION_PATTERNS) {
            if (lowerText.contains(pattern.lowercase())) return true
        }
        return false
    }

    private fun extractMiuiVersion(text: String?): String? {
        if (TextUtils.isEmpty(text)) return null
        val parts = text!!.split(" ")
        for (i in 0 until parts.size - 1) {
            if (parts[i].equalsIgnoreCase("MIUI") || parts[i].equalsIgnoreCase("HyperOS")) {
                return parts[i] + " " + parts[i + 1]
            }
        }
        return text
    }

    private fun extractVersionNumber(miuiVersion: String?): String? {
        if (TextUtils.isEmpty(miuiVersion)) return null
        val parts = miuiVersion!!.split(" ")
        if (parts.size > 1) {
            val versionPart = parts[1]
            val versionParts = versionPart.split("\\.".toRegex())
            if (versionParts.isNotEmpty()) return versionParts[0]
        }
        return null
    }

    private fun String.equalsIgnoreCase(other: String): Boolean {
        return this.equals(other, ignoreCase = true)
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String
        } catch (e: Exception) {
            Slog.w(TAG, "Error getting system property $key: ${e.message}")
            null
        }
    }
}
