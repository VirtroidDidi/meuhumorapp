package com.example.apphumor.models

import android.os.Parcel
import android.os.Parcelable


// Classe para representar os dados do usuário
data class User(
    var uid: String? = null,
    var nome: String? = null,
    var email: String? = null,
    var idade: Int? = null
    // outros campos...
)

// Classe para representar a nota de humor

data class HumorNote(
    var id: String? = null,
    var data: Map<String, Any> = emptyMap(),
    var humor: String? = null,
    var descricao: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        mutableMapOf<String, Any>().apply {
            val size = parcel.readInt()
            for (i in 0 until size) {
                put(parcel.readString()!!, parcel.readValue(Any::class.java.classLoader)!!)
            }
        },
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(data.size)
        data.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeValue(value)
        }
        parcel.writeString(humor)
        parcel.writeString(descricao)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<HumorNote> {
        override fun createFromParcel(parcel: Parcel): HumorNote {
            return HumorNote(parcel)
        }

        override fun newArray(size: Int): Array<HumorNote?> {
            return arrayOfNulls(size)
        }
    }

    val timestamp: Long
        get() = data["time"] as? Long ?: 0L
}
