//
//  SplashView.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import SwiftUI

struct SplashView: View {
    private var userDefaults = UserDefaults.standard
    // ログイン済み?
    @State var loggedin: Bool = !(UserDefaults.standard[.sk2Key] as String? ?? "").isEmpty
    // プライバシー承認済み?
    @State var acceptPolicy: Bool = UserDefaults.standard[.acceptPolicy] ?? false

    // アプリバージョン
    private let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as! String
    // ログイン済みバージョン
    private let loggedinBuild = UserDefaults.standard[.appBuild] ?? "1"

    var body: some View {
        NavigationView {
            ZStack {
                if loggedin && acceptPolicy {
                    MainView(loggedin: $loggedin, userId: userDefaults[.userId]!, userNameJP: userDefaults[.userNameJP]!)
                } else {
                    LoginView(loggedin: $loggedin, acceptPolicy: $acceptPolicy)
                }
                
//                Color.blue
//                    .edgesIgnoringSafeArea(.all)
//                Text("sk2")
//                    .font(.title)
//                    .fontWeight(.thin)
//                    .foregroundColor(Color.white)
            }
            .navigationBarTitle("")
            .navigationBarHidden(true)
        }
        // 全画面表示にする
        .navigationViewStyle(StackNavigationViewStyle())
//        .edgesIgnoringSafeArea(.all)
    }
}

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView()
    }
}
