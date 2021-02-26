//
//  PopupView.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import SwiftUI

struct PopupMessageView: View {
    var contents: (message: String, color: Color)
    
    var body: some View {
        Text(contents.message)
            .foregroundColor(contents.color)
            .fontWeight(.heavy)
            .padding(20)
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(contents.color, lineWidth: 5)
            )
    }
}

extension View {
    func popup<Content: View>(isPresented: Binding<Bool>, content: () -> Content) ->
        some View {
        if isPresented.wrappedValue {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                isPresented.wrappedValue = false
            }
        }
        return ZStack {
            self
            content()
                .opacity(isPresented.wrappedValue ? 1 : 0)
                .scaleEffect(isPresented.wrappedValue ? 1 : 0)
                .animation(.spring(response: 0.2, dampingFraction: 0.6, blendDuration: 0))
        }
    }
}

struct PopupView_Previews: PreviewProvider {
    static var previews: some View {
        PopupMessageView(contents: (message: "メッセージ", color: Color.red))
    }
}
