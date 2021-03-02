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
    @State private var showPrivacyModal = false
    // プライバシー承認シートの表示フラグ
    @State private var showPrivacyActionSheet = false
    // 組織ID
    let orgnization = 0
    // ログイン認証器
    @ObservedObject private var authenticator = LoginAuthenticator()
    
    var body: some View {
        VStack {
            Spacer(minLength:30)
            Text("龍谷大学先端理工学部 出席アプリ sk2")
                .font(.headline)
                .foregroundColor(Color.blue)
            
            Spacer(minLength: 30)
            VStack {
                TextField("全学認証ID", text: $id)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(5)
                    .keyboardType(.asciiCapable)
                    .autocapitalization(.none)
                SecureField("パスワード", text: $passwd)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(5)
                    .keyboardType(.asciiCapable)
                    .autocapitalization(.none)
                Spacer(minLength: 30)
                
                Button(action: {
                    loggedin = authenticator.challenge(orgnization: orgnization, id: id, passwd: passwd)
                    // ログイン時に毎回ポリシー確認
                    acceptPolicy = false
                    if loggedin {
                        showPrivacyModal = true
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
            .modifier(Shake(animatableData: CGFloat(attempts)))
            .keyboardObserving()
        }
        .popup(isPresented: $showFailPopup) {
            PopupMessageView(contents:(message:"ログイン失敗", color: Color.red))
        }
        .sheet(isPresented: $showPrivacyModal) {
            WebPrivacyView(showPrivacyActionSheet: $showPrivacyActionSheet)
                .actionSheet(isPresented: $showPrivacyActionSheet, content: {
                    ActionSheet(title: Text("理工出席プライバシーポリシーを承認"),
                                message: Text("承認しますか？"),
                                buttons: [
                                    .default(Text("承認します"), action: {
                                        acceptPolicy = true
                                        userDefaults[.acceptPolicy] = true
                                        showPrivacyModal = false
                                    }),
                                    .destructive(Text("承認しません"), action: {
                                        loggedin = false
                                        acceptPolicy = false
                                        userDefaults[.acceptPolicy] = false
                                        showPrivacyModal = false
                                    })
                        ]
                    )
                })
        }
    }
}

// Animated Shaker
struct Shake: GeometryEffect {
    var amount: CGFloat = 10
    var shakesPerUnit = 10
    var animatableData: CGFloat
    
    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX:
            amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)),
                                              y: 0))
    }
}
