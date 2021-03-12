package jp.ac.ryukoku.st.sk2

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.jakewharton.threetenabp.AndroidThreeTen
import org.jetbrains.anko.AnkoLogger

/** ////////////////////////////////////////////////////////////////////////////// **/
/** Application **/
class ApplicationContext: Application() {
    companion object {
        private var instance: ApplicationContext? = null
        var infoQueue = BeaconsQueue()
        fun applicationContext(): Context { return instance!!.applicationContext }
    }
    init {
        instance = this
        // Datetime ThreeTen
        AndroidThreeTen.init(this)
    }
}

/** ////////////////////////////////////////////////////////////////////////////// **/
class Sk2Globals: AnkoLogger {
    /** ////////////////////////////////////////////////////////////////////////////// **/
    companion object {
        const val APP_NAME = "sk2"
        const val APP_TITLE = "龍大先端理工学部出欠システム"

        const val RU_UUID = "ebf59ccc-21f2-4558-9488-00f2b388e5e6"
        const val SERVER_ORGNIZATION = 0

        /*** SharedPreferences の名前 ***/
        //const val SHARED_PREFERENCES_NAME = "st.ryukoku.sk3"
        /*** BLE Scan の実行自体を保存する SharedPreference のキー ***/
        const val SCAN_RUNNING = "scan_running"
        /*** login 情報の有効期限 ***/
        const val LOGIN_TIME_DAY_UNIT_MILLSEC: Long = 24*60*60*1000 // 1 days
        const val LOGIN_EXPIRY_PERIOD_DAYS: Long = 150

        /*** サーバ コマンドキー ***/
        //const val SERVER_COMMAND_AUTH = "AUTH"
        //const val SERVER_REPLY_AUTH_FAIL = "authfail"
        //const val SERVER_REPLY_FAIL = "fail"
        const val NAME_START_TESTUSER = "testuser"         // デバッグユーザー名の開始文字
        const val NAME_DEMOUSER = "$NAME_START_TESTUSER-demo"

        /*** View カラー ***/
        var COLOR_BACKGROUND = Color.parseColor("#fdfdfd") //#ecf0f1
        var COLOR_BACKGROUND_TITLE = Color.parseColor("#eeeeee")
        var COLOR_NORMAL = Color.parseColor("#2589cd") //#3498db
        var COLOR_NORMAL_LIGHT = Color.parseColor("#127ac0") //#2085c6
        var COLOR_HINT= Color.parseColor("#8a8a8a")
        //var COLOR_ACTIVE = Color.parseColor("#1abc9c")
        //var COLOR_ONDOWN = Color.parseColor("#2980b9")
        //var COLOR_DISABLE = Color.parseColor("#bdc3c7")

        /*** ScanService Extras ***/
        const val SCANSERVICE_EXTRA_SEND = "send_to_server"
        const val SCANSERVICE_EXTRA_ALARM = "from_alarm"

        /** Debug Info Message **/
        const val SCAN_INFO_NOT_FOUND = "Any Beacons not found."

        /*** Nortification ***/
        const val NOTIFICATION_ID: Int = 1
        const val CHANNEL_ID = "channel_sk2"
        const val CHANNEL_DESCRIPTION = "Sk2 Silent Notification"
        /*** Nortification メッセージ ***/
        const val NOTIFICATION_TITLE_TEXT = "sk2"
        const val NOTIFICATION_TEXT_STARTFORGROUND = "Foreground サービスを開始"
        const val NOTIFICATION_TEXT_SEND = "出席データを送信"

        /*** Timer ***/
        // BLEチェックのインターバル
        const val BLE_CHECK_INTERVAL_IN_MILLISEC: Long = 5000

        /*** Toast メッセージ ***/
        const val TEXT_OK = "OK"
        const val TEXT_SETTINGS = "設定"
        // Check BLE
        const val TOAST_CHECK_BLE_NON = "この端末のBLEアダプタが見つかりません"
        const val TOAST_CHECK_BLE_OFF = "Bluetoothをオンにしてください"
        // Location Permittion

        /*** 出席ボタンを押したときのバイブレーション継続時間 msec; 0 にするとエラー ***/
        const val ATTENDANCE_VIBRATE_MILLISEC: Long = 100
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/

    /*
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** ローカルキューを Json String に変換して SharedPreferences に保存 ***/
    fun saveQueue(clear: Boolean = false) {
        if (clear) localQueue = Queue()

        val gson = Gson()
        val jsonString = gson.toJson(localQueue)

        pref.edit()
            .putString(PREF_LOCAL_QUEUE, jsonString as String)
            .apply()
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** SharedPreferences からローカルキューの情報をリストア ***/
    fun restoreQueue() {
        val gson = Gson()
        val jsonString = pref.getString(PREF_LOCAL_QUEUE,
            gson.toJson(Queue<Triple<String, String, List<StatBeacon>>>(mutableListOf())))
        val type: Type = object: TypeToken<Queue<Triple<String, String, Collection<StatBeacon>>>>(){}.type
        val queue: Queue<Triple<String, String, List<StatBeacon>>> = gson.fromJson<Queue<Triple<String, String, List<StatBeacon>>>>(jsonString, type)

        /** remove recodes null data contained **/
        localQueue.clear()
        queue.items.forEach {r ->
            localQueue.push(r)
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** ログアウト時にユーザ情報を持つ全てのSharedPreferenceをクリア ***/
    fun readyTologout() {
        pref.edit()
            .putString(PREF_UID, "")
            .putString(PREF_KEY, "")
            .putString(PREF_USER_NAME, "")
            .putLong(PREF_LOGIN_TIME, 0L)
            .putBoolean(PREF_DEBUG, false)
            .putBoolean(PREF_AUTO, false)
            .apply()

        // Clear Local Queue
        saveQueue(clear = true)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** Auto Alarm が実行中か？ ***/
    fun getAutoRunning(): Boolean {
        return pref.getBoolean(PREF_AUTO, false)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** BLE デバイスがあるか？ ***/
    private fun hasBLE(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** BLE の有無とON/OFF をチェック ***/
    fun checkBt(): Boolean {
        val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (btAdapter == null || !hasBLE()) {
            toast(TOAST_CHECK_BLE_NON).setGravity(Gravity.CENTER, 0, 0)  // BLE がない
            return false
        } else if (!btAdapter.isEnabled) {
            toast(TOAST_CHECK_BLE_OFF).setGravity(Gravity.CENTER, 0, 0)  // BLE が OFF
            return false
        }
        return true
    }*/
}
