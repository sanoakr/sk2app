//
//  ClScanner.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import Foundation
import UIKit
import SwiftUI
import CoreLocation
import CoreBluetooth
import UserNotifications

//
// iBeacon 領域モニタリングクラス
//
class ClScanner: NSObject, ObservableObject {
    @Published var infos: Iqueue = Iqueue(maxLength: 100)
    @Published var userId: String!
    @Published var userName: String!

    // 送信テキスト
    var sendText: String = ""
    // 出席ボタン状態
    var buttonToggle = false
    // 手動送信完了フラグ
    var manualSendFinished = false
    // MainView の通知 Popup
    @Published var showPopup: Bool = false
    var popupContents: (String, Color) = ("nil", Color.blue)
    // Force Sending Sheet
    @Published var showForceSendSheet = false
    // MainView ActionSheet
    @Binding var showActionSheet: Bool

    // 詳細な位置情報許可の状態
    var disableLocationAuth: Bool = false
    // BLE のON/OFF 状態
    var disableBle: Bool = false
    // @Published gets 'Abort trap: 6' error.
    // It is probably too deep observed layer.

    static let `default` = ClScanner(showActionSheet: .constant(false))
    var locationManager: CLLocationManager!
    var cbCentralManager: CBCentralManager!
    
    // UserDefaults
    let userDefaults = UserDefaults.standard
    
    // iBeacon UUID
    let seta_uuid: UUID? = UUID(uuidString: "ebf59ccc-21f2-4558-9488-00f2b388e5e6")
    // Global モニタリング
    let setaIdentifier = -1 // Region/Area Identifire の Major (Int) に準拠
    var setaConstraint: CLBeaconIdentityConstraint!
    var setaRegion: CLBeaconRegion!
    // ビーコンセット
    //var beacons1: [Beacon] = []
    //var beacons2: [Beacon] = []
    // 領域セット(Area)
    var globalArea: Area!
    //var area1: Area!
    //var area2: Area!
    // Areaセット
    var areas: [Area] = []
    var currentArea: Area!
    
    // 最新の取得ビーコン情報
    var lastBeaconUpdate: Date = Date()
    var lastBeacons: [CLBeacon] = []
    // 最終（自動）送信日時
    var lastAutoSendDatetime: Date = Date()

    // 日時フォーマット
    let formatter = DateFormatter()
    let timeFormatter = DateFormatter()
    let startTime = "09:00:00"
    let endTime = "20:00:00"
    // Room Json
    let rooms = Rooms.default

    // イニシャライザ
    init(showActionSheet: Binding<Bool>) {
        self._showActionSheet = showActionSheet
        super.init()
        print("sk2 clScanner")
        
        // Info Queue
        //infos = Iqueue(maxLength: 100) // ログ表示の最大長
        
        // room JSON
        let json: String = userDefaults[.roomJSON] ?? ""
        rooms.add(data: try! JSONDecoder().decode([Room].self, from: json.data(using: .utf8)!))
        // ログ読み込み
        if let logJson: String = userDefaults[.logInfos] {
            print("logJson:", logJson)
            infos.set(log: try! JSONDecoder().decode([beaconLog].self, from: logJson.data(using: .utf8)!))
        }
        
        // Location Manager
        locationManager = CLLocationManager()
        locationManager.delegate = self

        // CoreBluetooth Central Manager
        cbCentralManager = CBCentralManager()
        cbCentralManager.delegate = self
        //disableBle = isDisableBle()

        // バックグラウンドフラグ
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.startMonitoringSignificantLocationChanges()
        // Global モニタリング・領域を設定
        setaConstraint = CLBeaconIdentityConstraint(uuid: seta_uuid!)
        setaRegion = CLBeaconRegion(beaconIdentityConstraint: setaConstraint, identifier: String(setaIdentifier))

        // セキュリティ認証のステータスを取得
        let status = locationManager.authorizationStatus
        // まだ認証が得られていない場合は、認証ダイアログを表示
        if(status == CLAuthorizationStatus.notDetermined) {
            locationManager.requestAlwaysAuthorization() // Always!!
        }
        // Notification のリクエスト
        UNUserNotificationCenter.current().requestAuthorization(options: [.badge, .sound, .alert]) { (granted, error) in
            DispatchQueue.main.async {
                if !granted {
                    _ = UIAlertController(title: "通知がOFF", message: "プッシュ通知がOFF。PUSHは届かない", preferredStyle: .alert)
                } else {
                    _ = UIAlertController(title: "通知ON", message: "プッシュ通知がON。PUSHが届く", preferredStyle: .alert)
                }
            }
        }
        // Notification リクエストをすべて削除
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()

        // ビーコンセットを定義して領域セットをつくる
        areas = makeRegionList(beaconCode: Rooms.default.getCode())
        // global 領域セット
        globalArea = makeGlobalRegions()
        // global 領域でモニタリング開始
        currentArea = globalArea
        //startRegion(area: globalArea) // しない
        //startRegion(regions: regions1)

        // 日時フォーマット
        formatter.locale = Locale(identifier: "ja_JP")
        formatter.dateStyle = .full
        formatter.timeStyle = .long
        timeFormatter.locale = Locale(identifier: "ja_JP")
        timeFormatter.dateFormat = "HH:mm:ss"
        
        // UserInfo
        let userDefaults = UserDefaults.standard
        userId = userDefaults[.userId]
        userName = userDefaults[.userName]
        if let ujson: Data  = userDefaults[.logInfos] {
            let logs: [beaconLog]? = try! JSONDecoder().decode([beaconLog].self, from: ujson) // Json 失敗で nil
            if let logs = logs {
                if logs.count < infos.count {
                    infos.set(log: logs)
                } else {
                    let slice: [beaconLog] = Array(logs.prefix(infos.max))
                    infos.set(log: slice)
                }
            }
        }
    }
    // Global Region を作成して regions に設定
    func makeGlobalRegions() -> Area {
        let regions = [Region(identifier: setaIdentifier, constraint: setaConstraint, region: setaRegion)]
        //let regions = [Region(beacons: [], identifier: setaIdentifier, constraint: setaConstraint, region: setaRegion)]
        // 領域の侵入Notificationを登録
        // Build Setting > Swift Compiler - Custom Flags > Active .. > Debug "DEBUG"
        #if DEBUG
        registerTriggerdNotification(region: setaRegion)
        #endif
        return Area(identifier: setaIdentifier, regions: regions)
    }
    // Rooms 辞書から Majorごとの Areaセットを作成
    func makeRegionList(beaconCode: Dictionary<Int, Dictionary<Int, Room>>) -> [Area] {
        var areas: [Area] = []
        for major in beaconCode.keys {
            //let strMajor = String(major)
            let constraint = CLBeaconIdentityConstraint(uuid: seta_uuid!, major: CLBeaconMajorValue(major))
            let region = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: String(major))

            // UUID + Major で定義するので beacons はカラのまま定義
            //let skRegion = Region(beacons: [], identifier: strMajor, constraint: constraint, region: region)
            let skRegion = Region(identifier: major, constraint: constraint, region: region)
            areas.append(Area(identifier: major, regions: [skRegion]))

            // 各領域の侵入Notificationを登録
            // Build Setting > Swift Compiler - Custom Flags > Active .. > Debug "DEBUG"
            #if DEBUG
            registerTriggerdNotification(region: region)
            #endif
        }
        return areas
    }
    // 領域に入ったときに発火する Notification を登録する
    func registerTriggerdNotification(region: CLRegion) {
        region.notifyOnEntry = true
        region.notifyOnExit = true
        let trigger = UNLocationNotificationTrigger(region: region, repeats: true)
        let content = UNMutableNotificationContent()
        content.title = "Region Triggerd: " + region.identifier
        content.body = region.identifier + " に入りました。"
        content.sound = UNNotificationSound.default
        let request = UNNotificationRequest(identifier: "immediately", content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
    // 日時付き dtprint(string: )
    func dtprint(string: String) {
        // Build Setting > Swift Compiler - Custom Flags > Active .. > Debug "DEBUG"
        #if DEBUG
            print("[" + formatter.string(from: Date()) + "] \(string)")
        #endif
    }
    //
    // 領域モニタリングの開始
    //
    func startRegion(area: Area) {
        // 既存のモニタ領域は止める addReagions() 必要??
        stopRegion()
        // 手動ボタン時は必ず送信メニューを表示
        if (self.buttonToggle) {
            self.buttonToggle = false
            self.showForceSendSheet = true
            self.showActionSheet = true
            self.objectWillChange.send()
            //
            self.manualSendFinished = false
            //
            
        }
        
        for r in area.regions {
            dtprint(string: "CL:Start Monitoring; " + r.identifier.description)
            locationManager.startMonitoring(for: r.region)
        }
        currentArea = area
        dtprint(string: "CL:Current Area; " + currentArea.identifier.description)
    }
    // ポップアップ送信
    func togglePopup(message: String, color: Color) {
        popupContents = (message, color)
        showPopup = true
        self.objectWillChange.send()
    }
    // 領域モニタリングの停止
    func stopRegion() {
        for r in locationManager.monitoredRegions {
            dtprint(string: "CL:Stop Monitoring; " + r.identifier)
            locationManager.stopMonitoring(for: r)
        }
        dtprint(string: "CL:Stop Monitoring; " + currentArea.identifier.description)
    }
    // CLBeacon の配列をパースする
    func clBeaconParse(beacons: [CLBeacon], typeSignal: sType, success: Bool, latitude: CLLocationDegrees, longitude: CLLocationDegrees) -> beaconLog? {
        //カラでもOK
        //if beacons.isEmpty { return nil }
        
        var datetime = formatter.string(from: Date())
        var major1: Int?, minor1: Int?, notes1: String?
        var major2: Int?, minor2: Int?, notes2: String?
        var major3: Int?, minor3: Int?, notes3: String?
        if beacons.count > 0 {
            datetime = formatter.string(from: beacons.first!.timestamp)
            major1 = Int(truncating: beacons[0].major)
            minor1 = Int(truncating: beacons[0].minor)
            notes1 = rooms.getRoom(major: major1!, minor: minor1!)
        }
        if beacons.count > 1 {
            major2 = Int(truncating: beacons[1].major)
            minor2 = Int(truncating: beacons[1].minor)
            notes2 = rooms.getRoom(major: major2!, minor: minor2!)
        }
        if beacons.count > 2 {
            major3 = Int(truncating: beacons[2].major)
            minor3 = Int(truncating: beacons[2].minor)
            notes3 = rooms.getRoom(major: major3!, minor: minor3!)
        }
        return beaconLog(Success: success, Datetime: datetime, Stype: typeSignal.rawValue,
                         Latitude: Float(latitude), Longitude: Float(longitude),
                         Major1: major1, Minor1: minor1, Notes1: notes1,
                         Major2: major2, Minor2: minor2, Notes2: notes2,
                         Major3: major3, Minor3: minor3, Notes3: notes3
        )
    }
    // Beacon 情報を追加
    func insertBeaconInfo(beacons: [CLBeacon], typeSignal: sType, success: Bool, latitude: CLLocationDegrees, longitude: CLLocationDegrees) {
        if let beaconLog = clBeaconParse(beacons: beacons, typeSignal: typeSignal, success: success, latitude: latitude, longitude: longitude) {
            infos.enqueue(element: beaconLog)
        }
        
        // MainView 更新
        self.objectWillChange.send()
        // ログデータ保存
        let logJson = try! JSONEncoder().encode(infos.list())
        userDefaults[.logInfos] = logJson
    }
//    // 領域トリガ情報を追加
//    func insertBeaconInfo(string: String) {
//        self.infos.enqueue(BeaconInfo(datetime: Date(), info: string, beacons: nil))
//        // MainView 更新
//        self.objectWillChange.send()
//    }
    
    // 最終更新日時を現在時刻から過去 rewindMinute 分前に巻き戻す（手動更新時などでインターバルを無視する）
    func rewindLastSendDatetime(rewindMinute: Int = 10) {
        self.lastAutoSendDatetime = Calendar.current.date(byAdding: .minute, value: -rewindMinute, to: Date())!
    }

    // Fail Reasons
    enum SendStat: Int {
        case success
        case short
        case overtime
        case emptybeacons
        case unknown_fail
    }
    // 送信処理
    func proceedSend(beacons: [CLBeacon], typeSignal: sType = .auto, manual: Bool = false) {
        // 送信ビーコンセット && 緯度経度
        var sendBeacons = beacons
        var latitude: CLLocationDegrees = 0
        var longitude: CLLocationDegrees = 0

        // 手動時に最新ビーコンセットが空だと送られないんじゃないか！ >>> 手動時に必ずスキャンが走るのでOK
        // 手動時は30秒以内の最新ビーコンセットを送信
        if manual && !lastBeacons.isEmpty && lastBeaconUpdate > Date(timeInterval: -30, since: Date()) {
            // 送信ビーコンセット
            sendBeacons = lastBeacons
            // 位置情報取得
            if let location = getCoordinateLocation() {
                latitude = location.latitude
                longitude = location.longitude
            }
        }
        // ビーコン情報送信
        let (stat, reply) = sendSk2Attend(beacons: sendBeacons, typeSignal: typeSignal, manual: manual, latitude: latitude, longitude: longitude)

        // 手動のときだけ表示
        if (buttonToggle) {
            if (stat == .emptybeacons && buttonToggle) {
            togglePopup(message: "出席ビーコンはありません", color: Color.red)
            }
            if (stat == .short) {
                togglePopup(message: "時間外です", color: Color.red)
            }
        }
        // 送信時処理
        if let reply = reply { // 送信トライしていたら reply がある
            dtprint(string: "sk2 send: \(reply)")
            if (stat == .success) { // 送信成功
                // ログ画面に成功として書き込み
                insertBeaconInfo(beacons: sendBeacons, typeSignal: typeSignal, success: true, latitude: latitude, longitude: longitude)
                togglePopup(message: "送信しました。", color: Color.blue)
            } else { // 失敗 .unknown_fail
                // ログ画面に失敗として書き込み
                insertBeaconInfo(beacons: sendBeacons, typeSignal: typeSignal, success: false, latitude: latitude, longitude: longitude)
                togglePopup(message: "送信に失敗しました。", color: Color.red)
            }
        }

        // ボタン通知をリセット
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.manualSendFinished = false
        }
    }
    
    // サーバへ出席情報を送信
    func sendSk2Attend(beacons: [CLBeacon], typeSignal: sType = .auto, manual: Bool = false, latitude: CLLocationDegrees = 0, longitude: CLLocationDegrees = 0) -> (SendStat, String?) {
        let now = Date()
        // 前回送信が5分未満なら送信しない // 手動のときは10分前に巻き戻しているので通過する
        if (now.timeIntervalSince(lastAutoSendDatetime) < 60 * 5) { // 5 minuites for testing
            dtprint(string: "\(#function): Too Short Interval")
            return (.short, nil)
        }
        // 時間外なら送信しない（テスト時なので止めてません）
        let nowTime = timeFormatter.string(from: now)
        if (nowTime < startTime || endTime < nowTime) {
            dtprint(string: "\(#function): Overtime")
//            return (.overtime, nil)
        }
        // ビーコンが空なら送信しない（手動送信時は無視）
        if (!manual && beacons.count == 0) {
            dtprint(string: "\(#function): Empty Beacons")
            return (.emptybeacons, nil)
        }
        // Connect & Send
        let attendSender = AttendSender()
        let (succeed, reply) = attendSender.send(beacons: beacons, userText: sendText, typeSignal: typeSignal, latitude: latitude, longitude: longitude)
        
        if let reply = reply {
            dtprint(string: "REPLY:\(reply)")
        }

        if succeed {
            lastAutoSendDatetime = Date() // 成功してたら最終送信日時を現在に更新
            return (.success, reply)
        } else {
            rewindLastSendDatetime(rewindMinute: 3) // 失敗なら3分前に巻き戻す
            return (.unknown_fail, reply)
        }
    }
    // 取得したビーコン情報からリージョンエリアを決める // GrobalArea.Identifiere -1 は利用しない
    func detectArea(beacons: [CLBeacon]) -> Area! {
        for b in beacons {
            // 最初に見つかった Unknown (0) じゃないビーコンでエリア判定する
            if b.proximity != CLProximity.unknown {
                let bMajor = CLBeaconMajorValue(truncating: b.major) // UInt16
                //let bMinor = CLBeaconMajorValue(truncating: b.minor)
                // area
                for area in areas {
                    // region
                    for region in area.regions {
                        // major
                        if region.identifier == Int(bMajor) {
                            return area
                        }
                    }
                }
            }
        }
        return nil
    }
    
    // 緯度経度情報を取得
    func getCoordinateLocation() -> CLLocationCoordinate2D? {
        // 取得開始
        locationManager.startUpdatingLocation()
        var coordinate: CLLocationCoordinate2D?

        coordinate = self.locationManager.location?.coordinate
        self.dtprint(string: "Location: \(String(describing: coordinate?.latitude)), \(String(describing: coordinate?.longitude))")
        // 停止
        self.locationManager.stopUpdatingLocation()

        return coordinate
    }
}
