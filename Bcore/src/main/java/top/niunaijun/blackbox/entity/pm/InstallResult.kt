package top.niunaijun.blackbox.entity.pm

import android.os.Parcel
import android.os.Parcelable
import top.niunaijun.blackbox.utils.Slog

open class InstallResult : Parcelable {

    @JvmField
    var success: Boolean = true

    @JvmField
    var packageName: String? = null

    @JvmField
    var msg: String? = null

    constructor()

    protected constructor(`in`: Parcel) {
        this.success = `in`.readByte().toInt() != 0
        this.packageName = `in`.readString()
        this.msg = `in`.readString()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (this.success) 1 else 0)
        dest.writeString(this.packageName)
        dest.writeString(this.msg)
    }

    fun installError(packageName: String?, msg: String?): InstallResult {
        this.msg = msg
        this.success = false
        this.packageName = packageName
        Slog.d(TAG, msg ?: "")
        return this
    }

    fun installError(msg: String?): InstallResult {
        this.msg = msg
        this.success = false
        Slog.d(TAG, msg ?: "")
        return this
    }

    companion object {
        const val TAG: String = "InstallResult"

        @JvmField
        val CREATOR: Parcelable.Creator<InstallResult> = object : Parcelable.Creator<InstallResult> {
            override fun createFromParcel(source: Parcel): InstallResult {
                return InstallResult(source)
            }

            override fun newArray(size: Int): Array<InstallResult?> {
                return arrayOfNulls(size)
            }
        }
    }
}
