package top.niunaijun.blackbox.entity

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

open class AppConfig : Parcelable {

    @JvmField
    var packageName: String? = null

    @JvmField
    var processName: String? = null

    @JvmField
    var bpid: Int = 0

    @JvmField
    var buid: Int = 0

    @JvmField
    var uid: Int = 0

    @JvmField
    var userId: Int = 0

    @JvmField
    var callingBUid: Int = 0

    @JvmField
    var token: IBinder? = null

    constructor()

    protected constructor(`in`: Parcel) {
        this.packageName = `in`.readString()
        this.processName = `in`.readString()
        this.bpid = `in`.readInt()
        this.buid = `in`.readInt()
        this.uid = `in`.readInt()
        this.userId = `in`.readInt()
        this.callingBUid = `in`.readInt()
        this.token = `in`.readStrongBinder()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.packageName)
        dest.writeString(this.processName)
        dest.writeInt(this.bpid)
        dest.writeInt(this.buid)
        dest.writeInt(this.uid)
        dest.writeInt(this.userId)
        dest.writeInt(this.callingBUid)
        dest.writeStrongBinder(token)
    }

    companion object {
        const val KEY: String = "BlackBox_client_config"

        @JvmField
        val CREATOR: Parcelable.Creator<AppConfig> = object : Parcelable.Creator<AppConfig> {
            override fun createFromParcel(source: Parcel): AppConfig {
                return AppConfig(source)
            }

            override fun newArray(size: Int): Array<AppConfig?> {
                return arrayOfNulls(size)
            }
        }
    }
}
