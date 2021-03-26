package jp.ac.ryukoku.st.sk2

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.view.Gravity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import jp.ac.ryukoku.st.sk2.ApplicationContext.Companion.infoQueue
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_AUTH_FAIL
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_FAIL
import jp.ac.ryukoku.st.sk2.Sk2Connector.Companion.REPLY_SUCCESS
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.RU_UUID
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.TEXT_OK
import jp.ac.ryukoku.st.sk2.Sk2Globals.Companion.TEXT_SETTINGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.altbeacon.beacon.*
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import org.jetbrains.anko.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class MainActivity : AppCompatActivity(), BeaconConsumer, AnkoLogger {
    companion object {
        private val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
        val ruRegion = Region((-1).toString(), Identifier.parse(RU_UUID), null, null)

        // トーストメッセージ
        const val TOAST_CANT_CONNECT_SERVER = "サーバに接続できません"
        const val TOAST_REPLY_SUCCESS = "送信完了！"
        const val TOAST_REPLY_FAIL = "送信に失敗しました"
        const val TOAST_REPLY_AUTH_FAIL = "認証に失敗しました"
        const val TOAST_SEND = "出席情報を送信します"

        const val TOAST_NO_BEACON = "ビーコンが見つかりません"
        const val TOAST_OUT_OF_TIME = "現在の時刻には送信できません"

        // 送信可能な時間範囲
        val from: LocalTime = LocalTime.of(8, 0)
        val to: LocalTime = LocalTime.of(20, 0)

        /*** パーミッション変更時のリクエストコード（任意の整数でよい） ***/
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val LOCATION_PERMISSION_REQUEST_MESSAGE = "位置情報へのアクセス権が必要です"
        const val LOCATION_PERMISSION_DENIED_MESSAGE = "位置情報へのアクセスが許可されませんでした"

    }

    // AltBeacon
    lateinit var beaconManager: BeaconManager
    lateinit var backgroundPowerSaver: BackgroundPowerSaver

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Pair<Double, Double> = Pair(0.0, 0.0)

    // UI
    private var mainUi = MainActivityUi()

    // 現在のスキャンRegion
    private var currentRegion = ruRegion

    // 最新のビーコン情報
    private var lastBeaconUpdated = LocalDateTime.now()
    private var lastBeacons = mutableListOf<Beacon>()

    // 最終の自動送信日時 // 1時間前
    private var lastAutoSendDatetime = LocalDateTime.now().minusHours(1)

    /** ///////////////////////////////////////////////////////////////////////// **/
    /** ///////////////////////////////////////////////////////////////////////// **/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** Location Client **/
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation() // 一度取得しておく

        /** AltBeacon Managerに接続 **/
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_FORMAT))
        beaconManager.bind(this)
        /** バックグラウンドスキャンはデフォルト 10sec/5min **/
        backgroundPowerSaver = BackgroundPowerSaver(this)
        // Android8 以降では JobScheduler の実行が制限を受け15分間隔(実際には10−25分)となる
        /** 1回のスキャン時間を20秒に伸ばす **/
        beaconManager.backgroundScanPeriod = 20000L

        /** JSONから教室情報を登録 **/
        Rooms.add(Sk2Preferences.getString(Sk2Preferences.Key.ROOM_JSON))
        /** 階層 Region を構成 **/
        Regions.set(Rooms)
        /** Preferences からログを読み込み **/
        infoQueue.fromJson(Sk2Preferences.getString(Sk2Preferences.Key.LOGS))
        info(infoQueue)

        /** UI **/
        mainUi.setContentView(this)
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** Disable Back Key **/
    override fun onBackPressed() {}

    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** ログアウト **/
    fun logout() {
        /** Scan 停止 **/
        stopMonitoring()
        beaconManager.unbind(this)

        /** Preferences クリア **/
        Sk2Preferences.clear(Sk2Preferences.Key.ACCEPT_POLICY)
        Sk2Preferences.clear(Sk2Preferences.Key.USER)
        Sk2Preferences.clear(Sk2Preferences.Key.SK2KEY)
        Sk2Preferences.clear(Sk2Preferences.Key.USER_NAME)
        Sk2Preferences.clear(Sk2Preferences.Key.USER_NAME_JP)
        Sk2Preferences.clear(Sk2Preferences.Key.ROOM_JSON)
        Sk2Preferences.clear(Sk2Preferences.Key.APP_VERSION)
        Sk2Preferences.clear(Sk2Preferences.Key.APP_CODE)
        Sk2Preferences.clear(Sk2Preferences.Key.LOGS)

        toast("ログアウトします").setGravity(Gravity.CENTER, 0, 0)

        /** ログイン画面へ **/
        startActivity(intentFor<LoginActivity>().clearTop())
    }

    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** Start/Stop Monitoring/Ranging **/
    fun startMonitoring(region: Region = ruRegion) {
        // 既存のモニタリングは全て停止
        stopMonitoring()
        // 指定モニタリングを開始
        info("Start Monitoring　$region")
        beaconManager.startMonitoringBeaconsInRegion(region)
        currentRegion = region

        startRanging(region)
    }

    fun stopMonitoring() {
        // Ranging も止める
        stopRanging()
        // モニタリング全部止める
        beaconManager.monitoredRegions.forEach {
            info("Stop Monitoring $it")
            beaconManager.stopMonitoringBeaconsInRegion(it)
        }
    }

    fun startRanging(region: Region) {
        // 既存のレンジングは全て停止
        stopRanging()
        // 指定レンジングを開始
        beaconManager.startRangingBeaconsInRegion(region)
        info("Start Ranging $region")
    }

    fun stopRanging() {
        // 既存のレンジングは全て停止
        beaconManager.rangedRegions.forEach {
            info("Stop Ranging $it")
            beaconManager.stopRangingBeaconsInRegion(it)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// **/
    /*** ビーコンサービスに接続できた ***/
    override fun onBeaconServiceConnect() {
        info("Connected to Beacon Service")
        /** 既存モニター通知を全削除 **/
        beaconManager.removeAllMonitorNotifiers()

        /** 領域モニターを開始 **/
        try {
            startMonitoring(currentRegion)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        /*** MonitorNotifier ***/
        /** ///////////////////////////////////////////////////////////////////////// **/
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            /** 領域に入った **/
            override fun didEnterRegion(region: Region?) {
                info("Enter Region ${region?.uniqueId}")
            }

            /** 領域から出た **/
            override fun didExitRegion(region: Region?) {
                info("Exit Region ${region?.uniqueId}")
                /** 領域からでたら必ず ruRegion のモニタリングから開始 **/
                startMonitoring(ruRegion)
            }

            /** 領域状態が変わった **/
            override fun didDetermineStateForRegion(state: Int, region: Region?) {
                info("Determined Region ${region?.uniqueId} to State $state")

                when (state) {
                    /** 領域内 **/
                    MonitorNotifier.INSIDE -> {
                        info("INSIDE: $currentRegion")
                        region?.let {
                            // 現在モニタリング中のRegion ならレンジング開始
                            if (region.uniqueId == currentRegion.uniqueId)
                                startRanging(currentRegion)
                        }
                    }
                    /** 領域外 **/
                    MonitorNotifier.OUTSIDE -> {
                        // ruRegion でなければ ruRegion から開始（ふりだしに戻る）
                        info("OUTSIDE: $currentRegion")
                        if (currentRegion != ruRegion) {
                            info("Current Region $currentRegion -> ruRegion")
                            currentRegion = ruRegion
                            startMonitoring(currentRegion)
                        }
                    }
                    /** ??? **/
                    else -> {
                        warn("Illegal MonitorNotifier State")
                    }
                }
            }
        })

        /*** RangingNotifier ***/
        /** ///////////////////////////////////////////////////////////////////////// **/
        beaconManager.addRangeNotifier { beacons, _ ->

            /** 領域のビーコンが見つかった **/
            if (!beacons.isNullOrEmpty()) {
                // RSSI でソート
                beacons.sortedBy { it.rssi }

                for (b in beacons)
                    info("Found a beacon ${b.id1}, ${b.id2}, ${b.id3} ${b.rssi}")

                // 最新のビーコン情報を更新
                lastBeaconUpdated = LocalDateTime.now()
                lastBeacons = beacons as MutableList<Beacon>
                // ビーコン情報を送信
                sendAttend(beacons)

                // 取得した最も近いビーコンでリージョンエリアを再設定する
                val detectedRegion = Regions.detectRegion(beacons.first())
                info("Detect Region $detectedRegion")
                info("Current Region $currentRegion")
                when (detectedRegion) {
                    currentRegion -> {
                    }
                    null -> {
                    } //ruRegion -> {}
                    else -> startMonitoring(detectedRegion)
                }
            }
        }
    }

    /** ////////////////////////////////////////////////////////////////////////////// **/
    @SuppressLint("MissingPermission")
    fun sendAttend(beacons: List<Beacon>?, text: String = String(),
                   typeSignal: SType = SType.AUTO, manual: Boolean = false): Pair<Boolean, String?> {
        /** Beacons **/
        var sendBeacons = beacons
        var sendLocation = Pair(0.0, 0.0)

        /** 送信日時 **/
        val now = LocalDateTime.now()
        var lastSendTime = lastAutoSendDatetime

        /** 手動送信の処理 **/
        if (manual) {
            // 手動時は最終送信日時を15分巻き戻し
            lastSendTime = lastAutoSendDatetime.minusMinutes(15)
            // ビーコン情報は30秒以内の取得情報を使う
            if (lastBeacons.isNotEmpty() && now.isBefore(lastBeaconUpdated.plusSeconds(30)))
                sendBeacons = lastBeacons
            /** Location **/
            getLastLocation()
            sendLocation = lastLocation
        }

        // 前回送信が10分未満なら送信しない // 手動のときは15分前に巻き戻しているので通過する
        if (now.isBefore(lastSendTime.plusMinutes(10))) {
            info("Attend Reply: Too Short Interval")
            return Pair(false, "short interval")
        }
        // 時間外で自動ならなら送信しない
        if ((now.toLocalTime() < from || to < now.toLocalTime()) && !manual) {
            info("Attend: Overtime")
            toast(TOAST_OUT_OF_TIME).setGravity(Gravity.CENTER, 0, 0)
            return Pair(false, "overtime")
        }
        // ビーコンが空なら送信しない（手動送信時は無視）
        if (!manual && beacons.isNullOrEmpty()) {
            info("Attend: Empty Beacons")
            toast(TOAST_NO_BEACON).setGravity(Gravity.CENTER, 0, 0)
            return Pair(false, "empty beacons")
        }
        /** 送信 **/
        toast(TOAST_SEND).setGravity(Gravity.CENTER, 0, 0)
        var reply: String? = null
        runBlocking(Dispatchers.IO) {
            reply = Sk2AttendSender.send(beacons, text, typeSignal, sendLocation, now)
            info("Attend Reply: $reply")
        }
        // Toast メッセージ
        when (reply) {
            REPLY_SUCCESS ->
                toast(TOAST_REPLY_SUCCESS).setGravity(Gravity.CENTER, 0, 0)
            REPLY_AUTH_FAIL ->
                toast(TOAST_REPLY_AUTH_FAIL).setGravity(Gravity.CENTER, 0, 0)
            REPLY_FAIL ->
                toast(TOAST_REPLY_FAIL).setGravity(Gravity.CENTER, 0, 0)
            null ->
                toast(TOAST_CANT_CONNECT_SERVER).setGravity(Gravity.CENTER, 0, 0)
            else ->
                toast(reply!!).setGravity(Gravity.CENTER, 0, 0)
        }

        reply?.let {
            // ビーコン情報をログに記録
            logBeacon(true, now, typeSignal, sendLocation, beacons)
            // LogView を更新
            mainUi.recAdapter.notifyDataSetChanged()
            // 最終送信日時を更新
            lastAutoSendDatetime = now

            return Pair(true, reply)
        }
        return Pair(false, reply)
    }

    /** ////////////////////////////////////////////////////////////////////////////// **/
    /** ビーコン情報をログに記録 **/
    private fun logBeacon(success: Boolean, now: LocalDateTime, typeSignal: SType,
                          sendLocation: Pair<Double?, Double?>, beacons: List<Beacon>?) {

        val info = BeaconLog(success, now.toString(), typeSignal)
        info.Latitude = sendLocation.first
        info.Longitude = sendLocation.second
        val beaconTriples = beaconsToTriples(beacons)
        beaconTriples.elementAtOrNull(0)?.let {
            info.Major1 = it.first; info.Minor1 = it.second; info.Room1 = it.third
        }
        beaconTriples.elementAtOrNull(1)?.let {
            info.Major2 = it.first; info.Minor2 = it.second; info.Room2 = it.third
        }
        beaconTriples.elementAtOrNull(3)?.let {
            info.Major3 = it.first; info.Minor3 = it.second; info.Room3 = it.third
        }

        infoQueue.push(info)
        // ログを Preferences に保存
        Sk2Preferences.setString(Sk2Preferences.Key.LOGS, infoQueue.toJson())
    }

    /** ////////////////////////////////////////////////////////////////////////////// **/
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        /** 位置情報へのパーミッションをチェック **/
        if (Build.VERSION.SDK_INT >= 30) {
            checkLocationPermission30(REQUEST_PERMISSIONS_REQUEST_CODE)
        } else if (Build.VERSION.SDK_INT >= 29) {
            checkLocationPermission29(REQUEST_PERMISSIONS_REQUEST_CODE)
        } else {
            checkLocationPermission28(REQUEST_PERMISSIONS_REQUEST_CODE)
        }

        /** 位置情報サービス Listner ： getLastLocation() で呼ばれる **/
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                /** 位置情報を更新 **/
                lastLocation = Pair(it.latitude, it.longitude)
            } else {
                // 位置情報が無かったら取りに行く
                val request = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(500)
                        .setFastestInterval(300)
                fusedLocationClient.requestLocationUpdates(
                        request,
                        object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                /** 位置情報を更新 **/
                                lastLocation = Pair(result.lastLocation.latitude, result.lastLocation.longitude)
                                // 現在地だけ欲しいので、1回取得したらすぐに外す
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        }, null
                )
            }
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションをチェック ***/
    private fun checkPermission(permission: String): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                permission
        )
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションをリクエスト <= 28 ***/
    private fun checkLocationPermission28(locationRequestCode: Int) {
        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                !checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            val permList = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            )
            requestPermissions(permList, locationRequestCode)
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションをリクエスト == 29 ***/
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkLocationPermission29(locationRequestCode: Int) {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
        val permList = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        requestPermissions(permList, locationRequestCode)

    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションをリクエスト == 30 ***/
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkLocationPermission30(locationRequestCode: Int) {
        if (checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
        AlertDialog.Builder(this)
                .setTitle("sk2はバックグラウンドでの位置情報を利用します")
                .setMessage("位置情報の権限で「常に許可」を選択します。設定画面に移動しますか？")
                .setPositiveButton("はい") { _, _ ->
                    // this request will take user to Application's Setting page
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), locationRequestCode)
                }
                .setNegativeButton("いいえ") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
    }
}
    /*
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションをリクエスト ***/

    private fun requestPermissions(permission: String) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            permission
        )
        if (shouldProvideRationale) {
            Snackbar.make(
                this.contentView!!,
                LOCATION_PERMISSION_REQUEST_MESSAGE,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(TEXT_OK) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(permission), REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }.show()
        } else {
            info("Requesting permission: $permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(permission), REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }
    /** ////////////////////////////////////////////////////////////////////////////// **/
    /*** パーミッションリクエストからの結果を処理 ***/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        info("onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() ->
                    info("User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission was granted.
                }
                else -> // Permission denied.
                    /** setButtonsState(false) **/
                    Snackbar.make(
                        this.contentView!!,
                        LOCATION_PERMISSION_DENIED_MESSAGE,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(TEXT_SETTINGS) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }.show()
            }
        }
    }
}
*/