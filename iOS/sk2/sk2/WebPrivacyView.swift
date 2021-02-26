//
//  WebPrivacyView.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import SwiftUI
import WebKit

struct WebPrivacyView: UIViewRepresentable {
    @Binding var showPrivacyActionSheet: Bool
    var webView = WKWebView()
    var loadUrl = "https://sk2.st.ryukoku.ac.jp/privacy.html"

    func makeCoordinator() -> WebPrivacyView.Coordinator {
        print("\(#function):\(showPrivacyActionSheet)")
        return Coordinator(showPrivacyActionSheet: $showPrivacyActionSheet)
    }
    
    func makeUIView(context: Context) -> WKWebView {
        webView.scrollView.delegate = context.coordinator
        return webView
    }
    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(URLRequest(url: URL(string: loadUrl)!))
    }
}

extension WebPrivacyView {
    class Coordinator: NSObject, UIScrollViewDelegate {
        @Binding var showPrivacyActionSheet: Bool
        var scrollCount = 0

        init(showPrivacyActionSheet: Binding<Bool>) {
            print("\(#function):\(showPrivacyActionSheet)")
            _showPrivacyActionSheet = showPrivacyActionSheet
        }
        func scrollViewDidScrollToTop(_ scrollView: UIScrollView) {
            print("\(#function):\(showPrivacyActionSheet)")
        }
        
        public func scrollViewDidScroll(_ scrollView: UIScrollView) {
            // 行末までスクロールした?
            if scrollView.contentSize.height - scrollView.contentOffset.y - scrollView.frame.size.height < 100 {
                print("the end")
                // あそびを入れないと表示時に行末判定される
                scrollCount += 1
                if scrollCount > 5 {
                    showPrivacyActionSheet = true
                }
            } else if scrollView.contentOffset.y < 100 {
            }
        }
        // [Process] kill() returned unexpected error 1 // iOS13 bug? on WKWebView Scrolling
        // https://www.yukiiworks.com/archives/390
    }
    
}
