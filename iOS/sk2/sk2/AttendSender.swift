//
//  AttendSender.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//
import Foundation
import CoreLocation

//
// 出席データー送信
//
class AttendSender {
    let userDefaults = UserDefaults.standard

    // 出席データを送信
    func send(beacons: [CLBeacon], userText: String, typeSignal: sType, latitude: CLLocationDegrees, longitude: CLLocationDegrees) -> (Bool, String?) {
        let message = makeAttend(beacons: beacons, userText: userText, typeSignal: typeSignal, latitude: latitude, longitude: longitude)
        
        // ネットワーク接続 // IP だと certification error
        let connector = Sk2Connector(message: message)
        // 受信完了まで待つ
        while(!connector.completed) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {}
        }
        // 成功してたらデータ取得
        if (connector.succeed) {
            let reply = connector.getDataString(encoding: .utf8)
            return (true, reply)
        } else {
            return (false, "")
        }
    }
    // ビーコン情報からメッセージ作成
    func makeAttend(beacons: [CLBeacon], userText: String, typeSignal: sType, latitude: CLLocationDegrees, longitude: CLLocationDegrees) -> String {
        let uid = userDefaults[.userId] as String?
        let key = userDefaults[.sk2Key] as String?
        let version = userDefaults[.appVersion] as String?
        var datetime = Date() // current datetime
        if !beacons.isEmpty { // Date() 現在時だけでいい？
            datetime = beacons.first!.timestamp
        }
        print(latitude.description)
        print(longitude.description)
        // 送信メッセージ
        var sendMessage = String()
        if let uid = uid, let key = key {
            sendMessage += "\(uid),\(key),\(version!),\(typeSignal.rawValue.description),\(userText),\(latitude.description),\(longitude.description),"
        }
        // 送信日時フォーマットはISO8601ext
        let sendFormatter = ISO8601DateFormatter()
        let sendDatetime = sendFormatter.string(from: datetime)
        //// 日時フォーマット yyy-MM-dd HH:mm:ss
        //let sendFormatter = DateFormatter()
        //sendFormatter.locale = Locale(identifier: "en_US_POSIX")
        //sendFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        //sendFormatter.timeZone = TimeZone(identifier: "Asia/Tokyo")
        
        sendMessage += "\(sendDatetime),"
        // Beacon Data // 最後に余分な , が入ります。
        for i in 0...2 {
            if i < beacons.count {
                let accuracy_dist = getDistance(proximity: beacons[i].proximity)
                sendMessage += "\(beacons[i].major),\(beacons[i].minor),\(accuracy_dist),"
            } else {
                sendMessage += ",,,"
            }
        }
        return sendMessage
    }
    
    // 距離計算 Proximity から推定（決め打ち）// CLBeacon には TxPower がなくて RSSI のみ
    func getDistance(proximity: CLProximity) -> Double {
        switch proximity {
        case .near:
            return 1.0
        case .immediate:
            return 3.0
        case .far:
            return 5.0
        case .unknown:
            return 99.0
        @unknown default:
            return -1.0
        }
    }
}
