//
//  Sk2Connector.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import Foundation
import Network

let SK2SERVER = "sk2.st.ryukoku.ac.jp"
let SK2PORT: UInt16 = 4440

class Sk2Connector {
    let connection: NWConnection
    var receiveData: Data!
//    var receiveMessage: String!
    var receiveCompleted: Bool
    
    init(ip: String = SK2SERVER, port: UInt16 = SK2PORT, message: String) {
        print("sk2 connector")
        let host = NWEndpoint.Host(ip)
        let port = NWEndpoint.Port(integerLiteral: port)
        receiveData = Data()
//        receiveMessage = String()
        receiveCompleted = false // 受信完了フラグ

        // TCP Options
        let tcpOptions = NWProtocolTCP.Options()
        tcpOptions.enableKeepalive = false
        tcpOptions.keepaliveIdle = 2 //sec
        tcpOptions.keepaliveCount = 2
        tcpOptions.keepaliveInterval = 2
        tcpOptions.connectionTimeout = 2
        tcpOptions.connectionDropTime = 2
        tcpOptions.persistTimeout = 2
        // TLS 接続子
        let tlsParams = NWParameters.init(tls: NWProtocolTLS.Options(), tcp: tcpOptions)
        connection = NWConnection(host: host, port: port, using: tlsParams)
        // ステータス監視ハンドラ
        connection.stateUpdateHandler = { (newState) in
            switch newState {
            case .ready:
                print("sk2 Connection ready to send")
                // 送信
                self.send(message: message)
                // 受信
                self.receive()
                
            case .waiting(let error):
                print("\(#function), \(error)")
                self.close()
            case .failed(let error):
                print("\(#function), \(error)")
                self.close()
            case .setup:
                print("sk2: Setup")
            case .cancelled: break
            case .preparing: break
            @unknown default:
                print("\(#function), Unknown error")
                self.close()
            }
        }

        // 5秒後に強制終了（tcpOptions の指定が効いてないので）
        let cancelDispachQueue = DispatchQueue(label: "cancel")
        cancelDispachQueue.asyncAfter(deadline: .now() + 5) {
            self.connection.forceCancel()
            print("force cancel connection with timeout")
        }

        // 接続!!
        print("sk2 start connection")
        let dispachQueue = DispatchQueue(label: "sk2")
        connection.start(queue: dispachQueue)
        // 受信
        //receive() // stateUpdateHandler に
    }

    // String を取得
    func getDataString(encoding: String.Encoding) -> String? {
        return String(bytes: receiveData, encoding: encoding)
    }

    // データ送信
    func send(message: String) {
        let data = message.data(using: .utf8)
        connection.send(content: data, completion: .contentProcessed { (error) in
            if let error = error {
                print("\(#function), \(error)")
            } else {
                print("Send: \(String(describing: data))")
                print(message)
            }
            
            })
    }

    // データ受信
    func receive() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: Int(UINT32_MAX), completion: { (data, context, isComplete, error) in

            if let data = data, !data.isEmpty {
                print("received \(data.count) bytes")
                self.receiveData.append(data)
//                self.receiveMessage += String(data: data, encoding: .utf8) ?? ""
            }
            if (isComplete) {
                print("finish connection")
                self.receiveCompleted = true
            } else if let error = error {
                print("receiving error \(error)")
                self.receiveCompleted = true
            } else {
                print("continue receiving")
                self.receive()
            }
        })
    }

    // 切断
    func close() {
        connection.cancel()
        print("sk2 close connection")
    }
}
