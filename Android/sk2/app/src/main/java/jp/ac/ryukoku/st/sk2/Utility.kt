package jp.ac.ryukoku.st.sk2

import me.mattak.moment.Moment
import java.lang.Math.pow
import java.util.*
import kotlin.math.pow

/** ////////////////////////////////////////////////////////////////////////////// **/
/*** 秒単位で日時の差を計算 ***/
fun differenceSec(m1: Moment, m2: Moment): Long {
    return m1.epoch - m2.epoch
}
/** ////////////////////////////////////////////////////////////////////////////// **/
/*** 曜日付きの日時文字列 ***/
fun getWeekDayString(moment: Moment): String {
    return moment.format("yyyy-MM-dd E HH:mm:ss ZZZZ")
}
/** ////////////////////////////////////////////////////////////////////////////// **/
/*** 日時文字列に曜日を挿入 ***/
fun addWeekday(dt: String?): String {
    var dwt = dt ?: ""

    try {
        val calendar = Calendar.getInstance()
        val match = Regex("(\\d+)-(\\d+)-(\\d+)T(\\d+):(\\d+):(\\d+).*").find(dwt)?.groupValues
        if (match?.size == 7) { // Null makes false
            val y = match[1].toInt()
            val m = match[2].toInt()-1
            val d = match[3].toInt()
            calendar.set(y, m, d, 0, 0, 0)
            val wday = Moment(calendar.time, TimeZone.getDefault(), Locale.JAPAN).weekdayName
            dwt = dwt.replaceFirst(" ", " $wday ").removeSuffix(" GMT+09:00")
        }
    }
    catch(e: Exception) { }

    return dwt
}
/** ////////////////////////////////////////////////////////////////////////////// **/
/** 有効なビーコン情報を含んでいるか **/
/*
fun hasValidBeacons(results: List<ScanResult>): Boolean {
    results.forEach { r ->
        val structures = ADPayloadParser.getInstance().parse(r.scanRecord?.bytes)
        structures.forEach { s ->
            if (s is IBeacon && (s.uuid.toString() in VALID_IBEACON_UUID))
                return true
        }
    }
    return false
}*/
/** ////////////////////////////////////////////////////////////////////////////// **/
/*** TxPower と RSSI からビーコンとの距離を計算 ***/
fun getBleDistance(tx: Int, rssi: Int, n: Double = 2.0): Double {
    return 10.0.pow((tx - rssi) / (n * 10))
}
/** ////////////////////////////////////////////////////////////////////////////// **/
/*** iOS 互換？ ***/
/*
fun iOSgetBleDistance(tx: Int, rssi: Int): Pair<Double, String> {
    if (rssi == 0)
        return Pair(-1.0, "Unknown")

    /** TxPower value on iOS to be mesured at 1m from a beacon. **/
    val ratio = rssi.toDouble()/(tx.toDouble()-41) // 1m 離れたぶんだけ減衰させる

    val dist = if (ratio < 1.0) Math.pow(10.0, ratio)
    else 0.89976 * Math.pow(ratio, 7.7095) + 0.111

    val ranging = if (dist < 1.0) "Immediate"
    else if (dist < 3.0) "Near"
    else if (dist < 20.0) "Far" // 20m は適当
    else "Unknown"

    return Pair(dist, ranging)
}
*/