//
//  ClScanner_CLextension.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import Foundation
import SwiftUI
import CoreLocation
import CoreBluetooth
import UserNotifications

// 領域モニタリング
//
extension ClScanner: CLLocationManagerDelegate {
    // Handling Errors
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        dtprint(string: "CL:Fail with Error \(error)")
    }
    func locationManager(_ manager: CLLocationManager, didFinishDeferredUpdatesWithError error: Error?) {
        if let e = error {
            dtprint(string: "CL:Finish Deferred Updates with Error \(e)")
        }
    }
    func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: Error) {
        if let r = region {
            dtprint(string: "CL:Monitering Fail Error: \(r), \(error)")
        } else {
            dtprint(string: "CL:Monitering Fail Error: Unknow, \(error)")
        }
    }
    // 位置情報の許可が変更された
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            disableLocationAuth = false
        case .notDetermined, .denied, .restricted:
            disableLocationAuth = true
        default:
            print("Unhandled case")
         }

         switch manager.accuracyAuthorization {
            case .reducedAccuracy:
                disableLocationAuth = true
            case .fullAccuracy:
                disableLocationAuth = false
           default:
              print("This should not happen!")
         }        
        // MainView 更新
        objectWillChange.send()
    }
    
    // 位置認証のステータスが変更された
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        dtprint(string: "CL:Authorization Changed")
        startRegion(area: currentArea, force: true)
    }

    // 位置情報更新が一時停止した、再開した
    func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
        dtprint(string: "CL:Pause Location Updates")
    }
    func locationManagerDidResumeLocationUpdates(_ manager: CLLocationManager) {
        dtprint(string: "CL:Resume Location Updates")
        //startRegion(area: currentArea)
    }
    // 位置情報がアップデートされた
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        dtprint(string: "CL:Update Locations")
        //startRegion(area: currentArea)
    }
    // モニタリングの開始に成功したら、領域判定する
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        dtprint(string: "CL:Monitering Successfully Started: " + region.identifier)
        locationManager.requestState(for: region)
        dtprint(string: "CL:Request State")
    }
    // 領域に入った: didEnterRegion > didDetermineState > didRangeBeacons
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        dtprint(string: "CL:Enter Region: " + region.identifier)
        // トリガーInfo
        //insertBeaconInfo(string: "Trigger: ENTER " + region.identifier)
    }
    //領域から出た: (didRangeBeacons) > didEExitRegion > didDetermineState
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        dtprint(string: "CL:Exit Region: " + region.identifier)
        // トリガーInfo
        //insertBeaconInfo(string: "Trigger: EXIT " + region.identifier)
        // Global へ
        startRegion(area: globalArea)
    }
    // 領域の状態が決定された
    func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for inRegion: CLRegion) {
        dtprint(string: "CL:Determined Reagion State")
        switch (state) {
        case .inside: // 領域内にいる
            dtprint(string: "CL:Determined Reagion - INSIDE: " + inRegion.identifier)
            // 現在エリアのリージョンがあればビーコン Ranging を開始
            for r in currentArea.regions {
                if Int(inRegion.identifier) == r.identifier {
                    locationManager.startRangingBeacons(satisfying: r.constraint)
                    dtprint(string: "CL:Start Ranging: " + r.identifier.description)
//                    // Send Notification
//                    requestEnterNotification(region: r)
                }
            }
            // トリガーInfo
            //insertBeaconInfo(string: "Trigger: IN REGION " + inRegion.identifier)
            break
        case .outside: // 領域外
            dtprint(string: "CL:Determined Reagion - OUTSIDE: " + inRegion.identifier)

            // 現在 Global モニタリングなら、return してそのまま Rangin を継続
            for cRegion in currentArea.regions {
                if cRegion.identifier == setaIdentifier {
                    return
                }
            }
            // Global でなければリージョンを停止して Global モニタリングへ
            // 現在エリアのリージョンがあればビーコン Ranging を止める
            for r in currentArea.regions {
                if Int(inRegion.identifier) == r.identifier {
                    locationManager.stopRangingBeacons(satisfying: r.constraint)
                    dtprint(string: "CL:Stop Ranging: " + r.identifier.description)
                }
            }
            // スキャン中で、Ranging 中のビーコンが無ければ Global モニタリング へ
            if locationManager.rangedBeaconConstraints.count == 0 && scanning {
                startRegion(area: globalArea)
            }
            // トリガーInfo
            //insertBeaconInfo(string: "Trigger: OUT OF REGION " + inRegion.identifier)
            break
        case .unknown: // わからん
            dtprint(string: "CL:Determined Reagion - UNKNOWN: " + inRegion.identifier)
            break
        }
    }
    
    func requestEnterNotification(region: Region) {
        let content = UNMutableNotificationContent()
        content.title = "didEnterRegion: " + region.identifier.description
        content.body = region.identifier.description + " に入りました。"
        content.sound = UNNotificationSound.default
        let request = UNNotificationRequest(identifier: "immediately", content: content, trigger: nil)
        // 残っているリクエストをすべて削除してから追加
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
    //
    // ビーコンからのRegioning情報が届いた
    //
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        //dtprint(string: "CL:Ranging \(beacons.count) beacon(s)")
        //insertBeaconInfo(beacons: beacons)
        
        // Sort Beacons by accuracy
        let sBeacons = beacons.sorted { (b1, b2) in return b1.accuracy > b2.accuracy }
        for b in sBeacons {
            dtprint(string: "\(b.timestamp), UUID:\(b.uuid), Major:\(b.major), Minor:\(b.minor), Proximity:\(b.proximity.stringValue), Accuracy:\(b.accuracy), RSSI:\(b.rssi)")
        }
        // 最新のビーコン情報（手動送信用に時間と保持)
        lastBeaconUpdate = Date()
        lastBeacons = sBeacons
        // ビーコン情報送信処理
        proceedSend(beacons: sBeacons)

        // 取得したビーコン情報からリージョンエリアを再設定する
        if scanning {
            let area = detectArea(beacons: sBeacons)
            if currentArea.identifier == setaIdentifier && area != nil {
                startRegion(area: area!)
                currentArea = area
            } else if currentArea.identifier != setaIdentifier && area == nil {
                startRegion(area: globalArea)
                currentArea = globalArea
            }
        }
    }
    // ロック中画面が表示された
    func locationManager(manager: CLLocationManager, didDetermineState state: CLRegionState, forRegion region: CLRegion) {
    }
}

// CoreBluetooth
extension ClScanner: CBCentralManagerDelegate {
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        disableBle = isDisableBle()
        // MainView 更新
        objectWillChange.send()
    }
    // BLE の状態確認とアラート
    func isDisableBle() -> Bool {
        if cbCentralManager.state != .poweredOn {
            //ここにOFFのときの処理を記載（アラートを表示など）
            print("Bluetooth: OFF")
            return true
        } else {
            print("Bluetooth: ON")
            return false
        }
    }
}
