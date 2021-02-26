//
//  dataStruct.swift
//  sk2
//
//  Created by sano on 2021/02/26.
//

import Foundation
import CoreLocation

// UserDefaults
enum Key: String {
    case sk2Key = "sk2_key"
    case orgnization = "orgnization"
    case userId = "user_id"
    case userName = "user_name"
    case userNameJP = "user_name_jp"
    case acceptPolicy = "accept_policy"
    case roomJSON = "room_json"
    case appVersion = "app_version"
    case appBuild = "app_build"
    case logInfos = "log_infos"
    //case array = "array"
    //case maxLength = "max_length"
}
extension UserDefaults {
    subscript<T: Any>(key: Key) -> T? {
        get {
            let value = object(forKey: key.rawValue)
            return value as? T
        }
        set {
            guard let newValue = newValue else {
                removeObject(forKey: key.rawValue)
                return
            }
            set(newValue, forKey: key.rawValue)
        }
    }
    func remove(_ key: Key) {
        removeObject(forKey: key.rawValue)
    }
    func hasKey(_ key: Key) -> Bool {
        return object(forKey: key.rawValue) != nil
    }
    
    func archive<T: NSSecureCoding>(key: String, value: T?) {
        if let value = value {
            self[Key(rawValue: key)!] = try? NSKeyedArchiver.archivedData(withRootObject: value, requiringSecureCoding: false)
        } else {
            self[Key(rawValue: key)!] = value
        }
    }
    func unarchive<T: NSSecureCoding>(key: String) -> T? {
        return data(forKey: key)
            .map { try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData($0) } as? T
    }
}
// Room JSON
struct Room: Codable {
    let Name: String?
    let Build: String?
    let Floor: String?
    let Room: String?
    let Major: Int?
    let Minor: Int?
    let Notes: String?
}
class Rooms {
    static let `default` = Rooms()
    private var data: [Room]
    private var code: Dictionary<Int, Dictionary<Int, Room>> // [Major: [Minor: Room]]
    init() { data = []; code = [:] }
    // 部屋を追加
    func add(data: [Room]) {
        self.data += data
        for room in data {
            if let major = room.Major, let minor = room.Minor {
                // 初出なら階層を作成
                if code[major] == nil {
                    code[major] = [:]
                }
                // null じゃないので force unwrap
                code[major]![minor] = room
            }
        }
    }
    // Major/Minor から部屋名
    func getRoom(major: Int, minor: Int) -> String {
        if let major = code[major] {
            if let minor = major[minor] {
                return minor.Notes ?? "This Room has No Name"
            }
        }
        return "Unknow Room"
    }
    // コーディングされた辞書を取得
    func getCode() -> Dictionary<Int, Dictionary<Int, Room>> {
        return code
    }
}

// Beacon Log struct
struct beaconLog: Codable, Identifiable {
    var id = UUID()
    let Success: Bool
    let Datetime: String?
    let Stype: Int?
    let Latitude: Float?
    let Longitude: Float?
    let Major1: Int?
    let Minor1: Int?
    let Notes1: String?
    let Major2: Int?
    let Minor2: Int?
    let Notes2: String?
    let Major3: Int?
    let Minor3: Int?
    let Notes3: String?
}
// Signal Type enum
enum sType: Int {
    case auto
    case off
    case seta
    case ryukoku
    
    func name() -> String {
        switch self {
        case .auto:
            return "Auto"
        case .off:
            return "Off Campus"
        case .seta:
            return "On Seta"
        case .ryukoku:
            return "On Ryukoku"
        }
    }
}
// Info Queue
class Iqueue {
    private var array: [beaconLog] = []
    private let maxLength: Int
    
    init(maxLength: Int) {
        self.maxLength = maxLength
    }
    public var isEmpty: Bool {
        return array.isEmpty
    }
    public var count: Int {
        return array.count
    }
    public func enqueue(element: beaconLog) {
        array.insert(element, at: 0)
        while (array.count > maxLength) {
            _ = dequeue()
        }
    }
    public func dequeue() -> beaconLog? {
        if isEmpty {
            return nil
        } else {
            return array.removeLast()
        }
    }
    public var front: beaconLog? {
        return array.first
    }
    public func list() -> [beaconLog] {
        return array
    }
    public var max: Int {
        return maxLength
    }
    public func set(log: [beaconLog]) {
        array = log
    }
}

// ビーコン情報
class Beacon {
    var identifier: String
    var major: CLBeaconMajorValue
    var minor: CLBeaconMinorValue
    init(identifier: String, major: CLBeaconMajorValue, minor: CLBeaconMinorValue) {
        self.identifier = identifier; self.major = major; self.minor = minor
    }
}
// 領域情報
class Region {
    //var beacons: [Beacon]
    var identifier: Int
    var constraint: CLBeaconIdentityConstraint
    var region: CLBeaconRegion
    init(//beacons: [Beacon],
        identifier: Int, constraint:     CLBeaconIdentityConstraint, region: CLBeaconRegion) {
        //self.beacons = beacons;
        self.identifier = identifier; self.constraint = constraint; self.region = region
    }
}
// エリア情報
class Area {
    var identifier: Int
    var regions: [Region]
    init(identifier: Int, regions: [Region]) {
        self.identifier = identifier; self.regions = regions
    }
}
