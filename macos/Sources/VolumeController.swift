import Foundation
import Combine

class VolumeController: ObservableObject {
    @Published var volume: Int = 50
    @Published var muted: Bool = false

    init() {
        self.volume = getVolume()
        self.muted = getMutedStatus()
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

            // Update state synchronously first, then notify UI
            self.volume = clampedVolume
            DispatchQueue.main.async {
                // Trigger UI update
                self.objectWillChange.send()
            }
        } catch {
            print("Error setting volume: \(error)")
        }
    }

    func getMutedStatus() -> Bool {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = ["-e", "output muted of (get volume settings)"]

        let pipe = Pipe()
        process.standardOutput = pipe

        do {
            try process.run()
            process.waitUntilExit()

            let data = pipe.fileHandleForReading.readDataToEndOfFile()
            if let output = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) {
                return output.lowercased() == "true"
            }
        } catch {
            print("Error getting muted status: \(error)")
        }

        return false
    }

    func setMuted(_ muted: Bool) {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = ["-e", "set volume output muted \(muted)"]

        do {
            try process.run()
            process.waitUntilExit()

            // Update state synchronously first, then notify UI
            self.muted = muted
            DispatchQueue.main.async {
                // Trigger UI update
                self.objectWillChange.send()
            }
        } catch {
            print("Error setting muted status: \(error)")
        }
    }
}
