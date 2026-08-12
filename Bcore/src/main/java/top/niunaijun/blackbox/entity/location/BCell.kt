package top.niunaijun.blackbox.entity.location

import android.os.Parcel
import android.os.Parcelable

class BCell : Parcelable {
    @JvmField public var MCC: Int = 0
    @JvmField public var MNC: Int = 0
    @JvmField public var LAC: Int = 0
    @JvmField public var CID: Int = 0
    @JvmField public var TYPE: Int = PHONE_TYPE_GSM

    constructor()

    constructor(MCC: Int, MNC: Int, LAC: Int, CID: Int) {
        this.TYPE = PHONE_TYPE_GSM
        this.MCC = MCC
        this.CID = CID
        this.MNC = MNC
        this.LAC = LAC
    }

    constructor(parcel: Parcel) {
        this.MCC = parcel.readInt()
        this.MNC = parcel.readInt()
        this.LAC = parcel.readInt()
        this.CID = parcel.readInt()
        this.TYPE = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.MCC)
        dest.writeInt(this.MNC)
        dest.writeInt(this.LAC)
        dest.writeInt(this.CID)
        dest.writeInt(this.TYPE)
    }

    override fun describeContents(): Int = 0

    companion object {
        const val NETWORK_TYPE_UNKNOWN = 0
        const val NETWORK_TYPE_GPRS = 1
        const val NETWORK_TYPE_EDGE = 2
        const val NETWORK_TYPE_UMTS = 3
        const val NETWORK_TYPE_CDMA = 4
        const val NETWORK_TYPE_EVDO_0 = 5
        const val NETWORK_TYPE_EVDO_A = 6
        const val NETWORK_TYPE_1xRTT = 7
        const val PHONE_TYPE_NONE = 0
        const val PHONE_TYPE_GSM = 1
        const val PHONE_TYPE_CDMA = 2

        @JvmField
        val CREATOR: Parcelable.Creator<BCell> = object : Parcelable.Creator<BCell> {
            override fun createFromParcel(source: Parcel): BCell = BCell(source)
            override fun newArray(size: Int): Array<BCell?> = arrayOfNulls(size)
        }
    }
}
