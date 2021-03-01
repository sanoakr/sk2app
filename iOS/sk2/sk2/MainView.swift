//
//  MainView.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import SwiftUI

import CoreLocation
import AudioToolbox
//import Combine
import KeyboardObserving

extension CLProximity {
    var stringValue: String {
        switch self {
        case .unknown:
            return "unknown"
        case .immediate:
            return "immediate"
        case .near:
            return "near"
        case .far:
            return "far"
        default:
            return "Unknown"
        }
    }
}

//struct BeaconsView: View {
//    let beacons: [clBeacon]
//    var body: some View {
//        List (beacons) { b in
//            HStack(alignment: .top) {
//                Text("Major: \(b.clBeacon.major)")
//                Text("Minor: \(b.clBeacon.minor)")
//                Text("Proximity: \(b.clBeacon.proximity.stringValue)")
//                //Text("Accuracy: \(b.clBeacon.accuracy)")
//                Text("RSSI: \(b.clBeacon.rssi)")
//            }.font(.caption)
//        }
//    }
//}
// nested List すると途中で表示されなくなる iOS13 のバグ?
// https://qiita.com/shou8/items/1d3454d176865adb457f
// https://github.com/ReactiveX/RxSwift/pull/2076

struct MainView: View {
    
    let userDefaults = UserDefaults.standard
    // Room JSON
    let rooms = Rooms.default
    
    @Binding var loggedin: Bool
    let userId: String
    let userNameJP: String
    
    // ActionSheet
    @State var showActionSheet = false
    @State var showLogoutSheet = false
    
    // アプリバージョン
    private let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as! String
    private let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
    
    @ObservedObject var clScanner = ClScanner.default
    //@ObservedObject private var restrictInput = RestrictInput(16) // 16文字制限
    @State private var sendText = ""
    
    var body: some View {
        VStack {
            HStack(spacing: -1) {
                Button(action: {
                    print("Button: Help")
                    if let url = URL(string: "https://sk2.st.ryukoku.ac.jp/") {
                        UIApplication.shared.open(url)
                    }
                }, label: {
                    Text("about sk2")
                        .foregroundColor(Color.blue)
                        .background(Color.white)
                        .padding(10)
                        .frame(maxWidth: .infinity)
                })
                .border(Color.blue, width: /*@START_MENU_TOKEN@*/1/*@END_MENU_TOKEN@*/)
                .frame(maxWidth: .infinity, alignment: .bottom)
                
                Button(action: {
                    print("Logout?")
                    self.showActionSheet = true
                    self.showLogoutSheet = true
                }, label: {
                    Text("Logout")
                        .foregroundColor(Color.blue)
                        .background(Color.white)
                        .padding(10)
                        .frame(maxWidth: .infinity)
                })
                .border(Color.blue, width: /*@START_MENU_TOKEN@*/1/*@END_MENU_TOKEN@*/)
                .frame(maxWidth: .infinity, alignment: .bottom)
            }
/*
            Divider()
            HStack {
                Spacer().frame(width:40)
                Button(action: {
                    print("Button: Search")
                    if let url = URL(string: "https://sk2.st.ryukoku.ac.jp/search/") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    VStack {
                        Image(systemName: "doc.text.magnifyingglass")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 20, height: 20)
                        Text("Log")
                            .font(.subheadline)
                    }
                }
                Spacer()
                Button(action: {
                    print("Button: Help")
                    if let url = URL(string: "https://sk2.st.ryukoku.ac.jp/") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    VStack {
                        Image(systemName: "info.circle")
                            //Image(systemName: "questionmark.square")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 20, height: 20)
                        Text("Help")
                            .font(.subheadline)
                    }
                }
                Spacer()
                
                Button(action: {
                    self.showActionSheet = true
                    self.showLogoutSheet = true
                    print("Logout?")
                }) {
                    VStack {
                        Image(systemName: "square.and.arrow.up")
                            .resizable()
                            .rotationEffect(.degrees(90))
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 20, height: 20)
                        Text("Logout")
                            .font(.subheadline)
                    }
                }
                Spacer().frame(width:30)
            }
            .padding(EdgeInsets(top: 10, leading:10, bottom:10, trailing:10))
*/
            //Divider()
            List {
                ForEach(clScanner.infos.list()) { info in
                    VStack(alignment: .leading) {
                        HStack {
                            Text(info.Datetime ?? "No Timestamp")
                                .font(.caption)
                                //.frame(maxWidth: .infinity, alignment: .leading)
                                .fontWeight(.semibold)
                                .foregroundColor(info.Success ? Color.blue : Color.red)
                            Spacer()
                            Text(sType(rawValue: info.Stype ?? 0)?.name() ?? "Unknown")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(Color.green)
                            
                            // Show Location
                            //if info.Stype != Int(0) {
                            //    Text("(\(String(format: "%.2f", Float(info.Latitude ?? 0))),\(String(format: "%.2f", Float(info.Longitude ?? 0))))")
                            //        .font(.caption)
                            //}
                        }
                        HStack {
                            Text((info.Notes1 ?? "No Beacons").replacingOccurrences(of: "_", with: " "))
                                .font(.caption)
                            Text("(\(String(Int(info.Major1 ?? -1))),\(String(Int(info.Minor1 ?? -1))))")
                                .font(.caption)
                            Spacer()
                        }
                        .padding(.leading, 5)
                        
                        if info.Notes2 != nil {
                            HStack {
                                Text(info.Notes2!.replacingOccurrences(of: "_", with: " "))
                                    .font(.caption)
                                Text("(\(String(Int(info.Major2 ?? -1))),\(String(Int(info.Minor2 ?? -1))))")
                                    .font(.caption)
                                Spacer()
                            }
                            .padding(.leading, 5)
                        }
                        if info.Notes3 != nil {
                            HStack {
                                Text(info.Notes3!.replacingOccurrences(of: "_", with: " "))
                                    .font(.caption)
                                Text("(\(String(Int(info.Major3 ?? -1))),\(String(Int(info.Minor3 ?? -1))))")
                                    .font(.caption)
                                Spacer()
                            }
                            .padding(.leading, 5)
                        }
                    }
                    .padding(EdgeInsets(top:0, leading:0, bottom:0, trailing: 0))
                }
            }
            .listStyle(PlainListStyle())
            
            TextField("送信文字列（max16文字）", text: $sendText,
                      onCommit: {
                        self.sendText = String(self.sendText.prefix(18))
                      })
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(5)
                .background(Color(UIColor.secondarySystemBackground))
                .keyboardType(.asciiCapable)
                .autocapitalization(.none)
                .keyboardObserving()
            
            Button(action: {
                print("Button")
                // 送信文字列を付与
                self.clScanner.sendText = self.sendText
                // ボタンを通知
                self.clScanner.buttonToggle = true
                // バイブレーション
                AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
                // ビーコン検索ポップアップ
                //self.clScanner.togglePopup(message: "ビーコンを検索します", color: Color.blue)
                self.showActionSheet = true
                self.clScanner.showForceSendSheet = true
                
            }, label: {
                Text("出席")
                    .font(.largeTitle)
                    .padding(20)
                    .frame(maxWidth: .infinity)
                    .foregroundColor(Color.white)
            })
            .frame(maxWidth: .infinity, alignment: .bottom)
            .background(self.clScanner.disableBle ? Color.gray : Color.blue)
            
            HStack {
                Text("\(userId) / \(userNameJP)")
                Spacer()
                Text("sk2 ver.\(version) (\(build))")
                    .font(.footnote)
            }
            .padding(5)
           
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // BLE アラート
        .alert(isPresented: self.$clScanner.disableBle, content: {
            Alert(title: Text("Bleutooth Status"), message: Text("Bleutooth をオンにしてください。"), dismissButton: .default(Text("OK")))
        })
        // 位置情報 アラート
        .alert(isPresented: self.$clScanner.disableLocationAuth, content: {
            Alert(title: Text("LocationAuth Status"), message: Text("ビーコン受信には詳細な位置情報が必要です。"), dismissButton: .default(Text("OK")))
        })
        // 送信時ポップアップ
        .popup(isPresented: $clScanner.showPopup) {
            PopupMessageView(contents: self.clScanner.popupContents)
        }
        // Action Sheet
        .actionSheet(isPresented: $showActionSheet, content: {
            self.generateActionSheet(showForceSendSheet: self.clScanner.showForceSendSheet)
        })
    }
    
    // ActionSheet Helper function
    func generateActionSheet(showForceSendSheet: Bool) -> ActionSheet {
        print(self.showActionSheet)
        print(self.showLogoutSheet)
        print(showForceSendSheet)
        if self.showLogoutSheet {
            print("Logout ActionSheet")
            // ログアウト確認シート
            return ActionSheet(title: Text("Logout"),
                               message: Text("ログアウトしますか？"),
                               buttons: [
                                .default(Text("ログアウトします"), action: {
                                    self.userDefaults[.userName] = String()
                                    self.userDefaults[.userNameJP] = String()
                                    self.userDefaults[.sk2Key] = String()
                                    self.loggedin = false
                                    self.showActionSheet = false
                                    self.showLogoutSheet = false
                                }),
                                .destructive(Text("キャンセル"), action: {
                                    self.showActionSheet = false
                                    self.showLogoutSheet = false
                                })
                               ]
            )
        } else if self.clScanner.showForceSendSheet {
            print("ForceSend ActionSheet")
            // 手動送信確認シート
            return ActionSheet(title: Text("Force Sending"),
                               message: Text("手動での出席送信を行いますか？"),
                               buttons: [
                                .default(Text("自宅・学外から送信します"), action: {
                                    self.manualSendProceed(typeSignal: sType.off)
                                }),
                                .default(Text("龍大瀬田キャンパスから送信します"), action: {
                                    self.manualSendProceed(typeSignal: sType.seta)
                                }),
                                .default(Text("その他の龍大施設から送信します"), action: {
                                    self.manualSendProceed(typeSignal: sType.ryukoku)
                                }),
                                .destructive(Text("キャンセル"), action: {
                                    self.showActionSheet = false
                                    self.clScanner.showForceSendSheet = false
                                })
                               ]
            )
        } else {
            self.clScanner.showForceSendSheet = false
            self.showLogoutSheet = false
            self.showActionSheet = false
            
            return ActionSheet(title: Text("dummy"))
        }
    }
    
    // 手動送信
    func manualSendProceed(typeSignal: sType) {
        // rewind 10 minuites before
        self.clScanner.rewindLastSendDatetime(rewindMinute: 10)
        // Restart Regioning for Sending
        self.clScanner.startRegion(area: self.clScanner.currentArea)
        
        // for
        if !self.clScanner.manualSendFinished {
            self.clScanner.proceedSend(beacons: [], typeSignal: typeSignal, manual: true)
        }
    }
}
