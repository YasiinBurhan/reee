package top.niunaijun.blackbox.entity.pm

import android.os.Parcel
import android.os.Parcelable

open class InstallOption : Parcelable {

    @JvmField
    var flags: Int = 0

    constructor()

    protected constructor(`in`: Parcel) {
        this.flags = `in`.readInt()
    }

    fun makeUriFile(): InstallOption {
        this.flags = this.flags or FLAG_URI_FILE
        return this
    }

    fun isFlag(flag: Int): Boolean {
        return (flags and flag) != 0
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.flags)
    }

    companion object {
        const val FLAG_SYSTEM: Int = 1
        const val FLAG_STORAGE: Int = 1 shl 1
        const val FLAG_URI_FILE: Int = 1 shl 3

        @JvmStatic
        fun installBySystem(): InstallOption {
            val installOption = InstallOption()
            installOption.flags = installOption.flags or FLAG_SYSTEM
            return installOption
        }

        @JvmStatic
        fun installByStorage(): InstallOption {
            val installOption = InstallOption()
            installOption.flags = installOption.flags or FLAG_STORAGE
            return installOption
        }

        @JvmField
        val CREATOR: Parcelable.Creator<InstallOption> = object : Parcelable.Creator<InstallOption> {
            override fun createFromParcel(source: Parcel): InstallOption {
                return InstallOption(source)
            }

            override fun newArray(size: Int): Array<InstallOption?> {
                return arrayOfNulls(size)
            }
        }
    }
}
