package jp.ac.ryukoku.st.sk2

import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.RU_UUID
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region

/** ////////////////////////////////////////////////////////////////////////////// **/
/** iBeacon 領域セット **/
object Regions {
    // 階層 Region
    var subRegions = mutableListOf<Region>()
    // 階層化マップ
    private var hierarchyMap = mutableMapOf<Int, MutableMap<Int, Room>>() // [Major: [Minor: Room]]

    // 教室追加
    fun set(room: Room): Boolean {
        if (room.Major != null && room.Minor != null) {
            /** 階層　Region に追加 **/
            val major = room.Major
            //val minor = room.Minor
            subRegions.add(Region(major.toString(), Identifier.parse(RU_UUID), Identifier.fromInt(major), null))

            /** 階層化マップに追加 **/
            // 初出なら階層を作成
            if (hierarchyMap[room.Major] == null)
                hierarchyMap[room.Major] = mutableMapOf<Int, Room>()

            // null じゃないので force unwrap
            hierarchyMap[room.Major]!![room.Minor] = room

            return true
        } else {
            return false
        }
    }
    // まとめて追加、返り値は Pair(成功、失敗)
    fun set(rooms: Rooms): Pair<Int, Int> {
        var success = 0
        var fail = 0
        rooms.getData().forEach {
            when (set(it)) {
                true -> { success += 1 }
                false -> { fail += 1 }
            }
        }
        return Pair(success, fail)
    }

    // ビーコン情報から階層 Region を取得
    fun detectRegion(beacon: Beacon): Region? {
        val major: Identifier = beacon.id2
        subRegions.forEach {
            if (major == it.id2) return it
        }
        return null
    }

    // 階層化コードマップを取得
    fun getHierarchyMap(): MutableMap<Int, MutableMap<Int, Room>> {
        return hierarchyMap
    }
}
