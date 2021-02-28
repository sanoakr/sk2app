package jp.ac.ryukoku.st.sk2

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.altbeacon.beacon.Beacon
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.lang.reflect.ParameterizedType

/** ////////////////////////////////////////////////////////////////////////////// **/
/** Signal Type enum **/
enum class SType(val rawValue: Int) {
    AUTO(0) {
        override fun toString() = "Auto"
        override fun message() = ""
    },
    OFF(1) {
        override fun toString() = "Off Campus"
        override fun message() = "自宅・学外から送信します"
    },
    SETA(2) {
        override fun toString() = "On Seta"
        override fun message() = "龍大瀬田キャンパスから送信します"
    },
    RYUKOKU(3) {
        override fun toString() = "On Ryukoku"
        override fun message() = "その他の龍大施設から送信します"
    };

    abstract fun message(): String

    companion object {
        fun fromInt(index: Int): SType {
            return values().firstOrNull { it.rawValue == index } ?: AUTO
        }
    }
}

/** Beacon Data **/
data class BeaconLog(
    val Success: Boolean,
    val Datetime: String,
    val Stype: SType,
    var Latitude: Double? = 0.0,
    var Longitude: Double? = 0.0,
    var Major1: Int? = null,
    var Minor1: Int? = null,
    var Room1: String? = null,
    var Major2: Int? = null,
    var Minor2: Int? = null,
    var Room2: String? = null,
    var Major3: Int? = null,
    var Minor3: Int? = null,
    var Room3: String? = null
)

/** ////////////////////////////////////////////////////////////////////////////// **/
/*** FIFO Queue: ローカルログ記録用 ***/
class BeaconsQueue(list: MutableList<BeaconLog> = mutableListOf(), size: Int = 100) {
    var items: MutableList<BeaconLog> = list
    private val maxsize = size        // Queue の最大長

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val type: ParameterizedType = Types.newParameterizedType(MutableList::class.java, BeaconLog::class.java)
    private val listAdapter: JsonAdapter<MutableList<BeaconLog>> = moshi.adapter(type)

    fun isEmpty():Boolean = items.isEmpty()
    fun count():Int = items.count()
    fun clear() = items.clear()
    fun get(postion: Int): BeaconLog = items[postion]
    //fun getItem(postion: Int):T = items[postion]
    override fun toString() = items.toString()
    /** //////////////////// **/
    fun push(element: BeaconLog){ // 先頭に PUSH
        items.add(0, element)
        if (count() > maxsize) {
            pop()
        }
    }
    /** //////////////////// **/
    fun pop(): BeaconLog? { // 最後から POP
        return if (this.isEmpty()){
            null
        } else {
            items.removeAt(items.lastIndex)
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /**fun peek():T?{ // 最後の要素を PEEK (消さない)
    return items.last()
    }**/
    /** ////////////////////////////////////////////////////////////////////////////// **/

    /** items to JSON **/
    fun toJson(): String {
        return listAdapter.toJson(items)
    }
    /** items from JSON **/
    fun fromJson(json: String) {
        //info("json: $json")
        if (json.isNotBlank())
            items = listAdapter.fromJson(json) ?: mutableListOf<BeaconLog>()
        //info("beacons: $items")
    }
}

/** altbeacon.beacon を Triple に変換する拡張関数 **/
fun Beacon.toTriple(): Triple<Int?, Int?, String?> {
    val major = this.id2.toInt()
    val minor = this.id3.toInt()
    return Triple(major, minor, Rooms.getRoom(major, minor))
}

/** List<beacon> を List<Triple> に変換 **/
fun beaconsToTriples(beacons: List<Beacon>?): MutableList<Triple<Int?, Int?, String?>> {
    val tripleList = mutableListOf<Triple<Int?, Int?, String?>>()
    beacons?.forEach {
        tripleList.add(it.toTriple())
    }
    return tripleList
}
