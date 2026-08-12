package top.niunaijun.blackbox.entity.pm

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import top.niunaijun.blackbox.BlackBoxCore

open class InstalledPackage : Parcelable {

    @JvmField
    var userId: Int = 0

    @JvmField
    var packageName: String? = null

    val application: ApplicationInfo?
        get() = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA, userId)

    val packageInfo: PackageInfo?
        get() = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA, userId)

    constructor()

    constructor(packageName: String?) {
        this.packageName = packageName
    }

    protected constructor(`in`: Parcel) {
        this.userId = `in`.readInt()
        this.packageName = `in`.readString()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.userId)
        dest.writeString(this.packageName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstalledPackage) return false
        return packageName == other.packageName
    }

    override fun hashCode(): Int {
        return packageName?.hashCode() ?: 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InstalledPackage> = object : Parcelable.Creator<InstalledPackage> {
            override fun createFromParcel(source: Parcel): InstalledPackage {
                return InstalledPackage(source)
            }

            override fun newArray(size: Int): Array<InstalledPackage?> {
                return arrayOfNulls(size)
            }
        }
    }
}
