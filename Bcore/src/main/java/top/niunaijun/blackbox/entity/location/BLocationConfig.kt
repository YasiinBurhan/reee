package top.niunaijun.blackbox.entity.location

import android.os.Parcel
import android.os.Parcelable

class BLocationConfig : Parcelable {
    @JvmField var pattern: Int = 0
    @JvmField var cell: BCell? = null
    @JvmField var allCell: List<BCell>? = null
    @JvmField var neighboringCellInfo: List<BCell>? = null
    @JvmField var location: BLocation? = null

    constructor()

    constructor(parcel: Parcel) {
        refresh(parcel)
    }

    fun refresh(parcel: Parcel) {
        this.pattern = parcel.readInt()
        this.cell = parcel.readParcelable(BCell::class.java.classLoader)
        this.allCell = parcel.createTypedArrayList(BCell.CREATOR)
        this.neighboringCellInfo = parcel.createTypedArrayList(BCell.CREATOR)
        this.location = parcel.readParcelable(BLocation::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.pattern)
        dest.writeParcelable(this.cell, flags)
        dest.writeTypedList(this.allCell)
        dest.writeTypedList(this.neighboringCellInfo)
        dest.writeParcelable(this.location, flags)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BLocationConfig> = object : Parcelable.Creator<BLocationConfig> {
            override fun createFromParcel(source: Parcel): BLocationConfig = BLocationConfig(source)
            override fun newArray(size: Int): Array<BLocationConfig?> = arrayOfNulls(size)
        }
    }
}
