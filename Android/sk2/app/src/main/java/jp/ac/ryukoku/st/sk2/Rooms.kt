package jp.ac.ryukoku.st.sk2

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/** ////////////////////////////////////////////////////////////////////////////// **/
/** Room JSON **/
data class Room(
    val Name: String?,
    val Build: String?,
    val Floor: String?,
    val Room: String?,
    val Major: Int?,
    val Minor: Int?,
    val Notes: String?
)
/** Singleton Object for Rooms */
object Rooms: AnkoLogger {
    // Room データリスト
    private var data = mutableListOf<Room>()
    // revers マップ
    private var roomMap = mutableMapOf<Pair<Int?, Int?>, Room>()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val type = Types.newParameterizedType(List::class.java, Room::class.java)
    val listAdapter: JsonAdapter<List<Room>> = moshi.adapter(type)

    // Json からデータ追加
    fun add(json: String) {
        info("json $json")
        val roomsList = listAdapter.fromJson(json)
        info("roomsList $roomsList")

        roomsList?.forEach { add(it) }
    }
    // データ追加
    private fun add(room: Room) {
        // Room リストへ追加
        data.add(room)
        // revers Map
        if (room.Major != null && room.Minor != null )
            roomMap[Pair(room.Major, room.Minor)] = room
    }

    // Major/Minor から部屋名を取得
    fun getRoom(major: Int, minor: Int): String {
        return roomMap[Pair(major, minor)]?.Notes ?: "No Name"
    }

    // dataList 取得
    fun getData(): MutableList<Room> {
        return data
    }
}
