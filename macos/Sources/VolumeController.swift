import Foundation
import Combine

class VolumeController: ObservableObject {
    @Published var volume: Int = 50

    init() {
        self.volume = getVolume()
    }

    func getVolume() -> Int {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = ["-e", "output volume of (get volume settings)"]

        let pipe = Pipe()
        process.standardOutput = pipe

        do {
            try process.run()
            process.waitUntilExit()

            let data = pipe.fileHandleForReading.readDataToEndOfFile()
            if let output = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) {
                return Int(output) ?? 50
            }
        } catch {
            print("Error getting volume: \(error)")
        }

        return 50
    }

    func setVolume(_ vol: Int) {
        let clampedVolume = min(100, max(0, vol))

        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = ["-e", "set volume output volume \(clampedVolume)"]

        do {
            try process.run()
            process.waitUntilExit()

            DispatchQueue.main.async {
                self.volume = clampedVolume
            }
        } catch {
            print("Error setting volume: \(error)")
        }
    }
}
