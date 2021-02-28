package jp.ac.ryukoku.st.sk2

import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_FAIL
import jp.ac.ryukoku.st.sk2.Sk2Preferences.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.altbeacon.beacon.Beacon
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

object Sk2AttendSender: AnkoLogger {
    // ISO8601 formatter
    private val sendFormatter = DateTimeFormatter.ISO_INSTANT

    /** 出席データを送信 **/
    suspend fun send(beacons: List<Beacon>?, userText: String, typeSignal: SType,
                     location: Pair<Double, Double>, datetime: LocalDateTime): String? {
        // 送信メッセージを作成
        val message = makeAttendMessage(beacons, userText, typeSignal, location, datetime)
        info(message)

        // 返り値が欲しいので非同期する意味ないよね?
        // > MainThread でネットワーク接続すると NetworkOnMainThreadException がスローされる
        var result: String? = null
        message?.let {
            // Connector
            val connector = Sk2Connector()

            result = if (connector.connect()) {
                connector.apply(message)
            } else {
                REPLY_FAIL
            }
            connector.close()
        }
        return result
    }

    /** ビーコン情報からメッセージを作成 **/
    fun makeAttendMessage(beacons: List<Beacon>?, userText: String, typeSignal: SType,
                          location: Pair<Double, Double>, datetime: LocalDateTime): String? {
        val uid = getString(Sk2Preferences.Key.USER)
        val key = getString(Sk2Preferences.Key.SK2KEY)
        val version = getString(Sk2Preferences.Key.APP_VERSION)
        val latitude: String = "%.2f".format(location.first)
        val longitude: String = "%.2f".format(location.second)
        // to ISO8601 UTC datetime
        val sendDatetime: String = datetime.atZone(ZoneOffset.UTC).format(sendFormatter)

        info("$uid, $key, $version, $location")
        // 送信メッセージ
        var sendMessage = String()
        if (! uid.isNullOrBlank() && ! key.isNullOrBlank()) {
            sendMessage += "$uid,$key,"
            sendMessage += "$version,${typeSignal.rawValue},$userText,"
            sendMessage += "$latitude,$longitude,$sendDatetime,"

            for (i in 0..2) {
                sendMessage += if (i < beacons?.count() ?: 0) {
                    "${beacons!![i].id2},${beacons[i].id3},${"%.2f".format(getBleDistance(beacons[i].txPower, beacons[i].rssi))},"
                } else {
                    ",,,"
                }
            }
        }
        else {
            return null
        }
        return sendMessage
    }
}