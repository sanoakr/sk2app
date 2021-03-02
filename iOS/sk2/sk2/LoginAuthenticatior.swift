//
//  LoginAuthenticatior.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import Foundation

//
// ログイン認証
//
class LoginAuthenticator: ObservableObject {
    //@Published var loginSucceed: Bool = false
    @Published var userName: String = "nil"
    @Published var userNameJP: String = "nil"
    
    // Room JSON
    let rooms = Rooms.default
    
    // ログイン認証チャレンジ
    func challenge(orgnization: Int, id: String, passwd: String) -> Bool {
        print("sk2", id, passwd)
        
        let userDefaults = UserDefaults.standard
        userDefaults[.orgnization] = orgnization.description
        userDefaults[.userId] = id
        
        let message = "AUTH,\(orgnization.description),\(id),\(passwd)"
        var succeed = false
        // ログイン認証
        // ネットワーク接続 // IP だと certification error
        let connector = Sk2Connector(message: message)
        // 受信完了まで待つ
        while(!connector.completed) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {}
        }
        // 成功してたらデータ取得
        if (connector.succeed) {
            // 受信処理
            if let receiveString = connector.getDataString(encoding: .utf8) {
                if (receiveString.starts(with:"authfail")) {
                    succeed = false
                } else {
                    let subStrings = receiveString.split(separator: ",", maxSplits: 3, omittingEmptySubsequences: false)
                    userDefaults[.sk2Key] = subStrings[0]
                    userDefaults[.userName] = subStrings[1]
                    userDefaults[.userNameJP] = subStrings[2]
                    userDefaults[.roomJSON] = subStrings[3]
                    // ユーザー情報を View へ publish
                    userName = userDefaults[.userName]!
                    userNameJP = userDefaults[.userNameJP]!
                    // アプリバージョン > MainView 表示したら更新
                    //userDefaults[.appBuild] = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion")
                    //userDefaults[.appVersion] = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString")

                    succeed = true
                }
            } else { succeed = false } // 受信データがない
        } else { succeed = false } // 送受信失敗
        
        connector.close()
        return succeed
    }
}
