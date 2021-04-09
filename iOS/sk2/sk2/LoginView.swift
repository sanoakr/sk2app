//
//  LoginView.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import SwiftUI
import KeyboardObserving

struct LoginView: View {
    @Binding var loggedin: Bool
    @Binding var acceptPolicy: Bool
    
    var userDefaults = UserDefaults.standard
    @State private var id : String = UserDefaults.standard[.userId] ?? ""
    // アプリバージョン
    private let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as! String
    private let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
    @State private var passwd = ""
    // ログインチャレンジの失敗数
    @State private var attempts : Int = 0
    // ログイン失敗ポップアップ
    @State private var showFailPopup = false
    // プライバシーモーダルの表示フラグ
    //@State private var showPrivacyModal = false
    // プライバシー承認シートの表示フラグ
    @State private var showPrivacyActionSheet = false
    
    // 組織ID
    let orgnization = 0
    // ログイン認証器
    @ObservedObject private var authenticator = LoginAuthenticator()
    
    // Sheet enum
    enum PresentSheet {
        case explain0
        case explain1
        case privacy
    }
    // SheetView
    @State private var presentSheet: PresentSheet = .explain0
    @State private var showSheet = false
    
    var body: some View {
        VStack {
            Spacer(minLength:30)
            Text("龍谷大学先端理工学部 出席アプリ sk2")
                .font(.headline)
                .foregroundColor(Color.blue)
            
            Spacer(minLength: 30)
            VStack {
                CustomTextField(placeholder: Text("全学認証ID").foregroundColor(.blue), text: $id, secure: false)
                CustomTextField(placeholder: Text("パスワード").foregroundColor(.blue), text: $passwd, secure: true)
                Spacer(minLength: 30)
                
                Button(action: {
                    // loggiedin && acceptPolicy で SplashView から MainView へ遷移
                    loggedin = authenticator.challenge(orgnization: orgnization, id: id, passwd: passwd)
                    acceptPolicy = false
                    
                    if loggedin {
                        // バージョン更新がなくてポリシー承認済みなら MainView へ
                        if build == userDefaults[.appBuild] && version == userDefaults[.appVersion] && (userDefaults[.acceptPolicy] ?? false) {
                            acceptPolicy = true
                        } else {
                            //showPrivacyModal = true
                            showSheet = true
                        }
                    } else {
                        showFailPopup = true
                    }
                    withAnimation(.default) {
                        attempts += 1
                        if attempts > 5 {
                            id = ""; passwd = ""
                            attempts = 0
                        }
                    }
                }) {
                    Text(" ログイン ")
                        .fontWeight(.bold)
                        .padding()
                        .overlay(
                            Capsule(style: .continuous)
                                .stroke(Color.blue, lineWidth: 2)
                        )
                }
                Spacer();
                HStack {
                    Spacer()
                    Text("sk2 ver.\(version) (\(build))")
                        .font(.footnote)
                }
                .padding(5)
            }
            .padding(10)
            .modifier(Shake(animatableData: CGFloat(attempts)))
            .keyboardObserving()
        }
        .popup(isPresented: $showFailPopup) {
            PopupMessageView(contents:(message:"ログイン失敗", color: Color.red))
        }
        .sheet(isPresented: $showSheet) {
            if (presentSheet == .explain0) {
                VStack {
                    Text("sk2 は教室位置情報をバックグラウンドで取得します")
                        .font(.title2)
                        .foregroundColor(Color.blue)
                        .padding(8)
                    Image("explain1")
                        .resizable()
                        .scaledToFit()
                        .frame(width:200, height:150)
                    
                    VStack(alignment: .leading) {
                        ListedText(text: "sk2アプリを一度起動しておけば、自動で出席記録が行われます。")
                        ListedText(text: "教室移動などでビーコン信号が変化すると自動で出席情報が送信されます。")
                        ListedText(text: "自動送信は、龍谷大学瀬田キャンパス内で、授業実施時間にのみ実行されます。")
                        ListedText(text: "自動送信の記録頻度は最短でも10分に1回です。")
                    }
                    Button("次へ") {
                        presentSheet = .explain1
                    }
                    .font(.headline)
                }
                .padding(32)
                
            } else if (presentSheet == .explain1) {
                VStack {
                    Text("sk2 は手動送信でのみ、緯度経度を含むおおよその位置情報を取得します")
                        .font(.title2)
                        .foregroundColor(Color.blue)
                        .padding(8)
                    Image("explain2")
                        .resizable()
                        .scaledToFit()
                        .frame(width:200, height:150)
                    
                    VStack(alignment: .leading) {
                        ListedText(text: "手動送信は「出席」ボタンで表示される「送信場所」を選択すると実行されます。")
                        ListedText(text: "手動記録では、GPSや基地局情報などからおおよその緯度経度情報が送信されます。")
                        ListedText(text: "手動記録は、オンライン授業での出席記録や災害時の安否確認などに利用されます。")
                    }
                    Button("次へ") {
                        presentSheet = .privacy
                    }
                    .font(.headline)
                }
                .padding(32)
                
            } else if (presentSheet == .privacy) {
                WebPrivacyView(showPrivacyActionSheet: $showPrivacyActionSheet)
                    .actionSheet(isPresented: $showPrivacyActionSheet, content: {
                        ActionSheet(title: Text("先端理工出席sk2プライバシーポリシーを承認"),
                                    message: Text("承認しますか？"),
                                    buttons: [
                                        .default(Text("承認します"), action: {
                                            acceptPolicy = true
                                            userDefaults[.acceptPolicy] = true
                                            userDefaults[.appBuild] = build
                                            userDefaults[.appVersion] = version
                                            showSheet = false
                                        }),
                                        .destructive(Text("承認しません"), action: {
                                            loggedin = false
                                            acceptPolicy = false
                                            userDefaults[.acceptPolicy] = false
                                            showSheet = false
                                            presentSheet = .explain0
                                        })
                                    ]
                        )
                    })
            }
        }
    }
}
// CustomTextField
struct CustomTextField: View {
    var placeholder: Text
    @Binding var text: String
    var editingChanged: (Bool)->() = { _ in }
    var commit: ()->() = { }
    var secure: Bool

    var body: some View {
        ZStack(alignment: .leading) {
            if secure {
                SecureField("", text: $text, onCommit: commit)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.asciiCapable)
                    .autocapitalization(.none)
            } else {
                TextField("", text: $text, onEditingChanged: editingChanged, onCommit: commit)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.asciiCapable)
                    .autocapitalization(.none)
                }

            if text.isEmpty { placeholder.padding(10) }
        }
    }
}
// ListedText for Explanation Sheets
struct ListedText: View {
    var text: String
    
    var body: some View {
        HStack(alignment: .top) {
            Text("● ")
                .foregroundColor(Color.blue)
            Text(text)
                .font(.body)
        }
        .padding(.vertical)
    }
}
// Animated Shaker
struct Shake: GeometryEffect {
    var amount: CGFloat = 10
    var shakesPerUnit = 10
    var animatableData: CGFloat
    
    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX:
                                                amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)), y: 0))
    }
}
