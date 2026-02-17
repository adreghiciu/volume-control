import SwiftUI

struct VolumeView: View {
    @ObservedObject var volumeController: VolumeController

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Volume")
                    .font(.headline)
                Spacer()
            }

            Slider(value: .init(
                get: { Double(volumeController.volume) },
                set: { volumeController.setVolume(Int($0)) }
            ), in: 0...100, step: 1)

            HStack {
                Text("\(volumeController.volume)%")
                    .font(.caption)
                Spacer()
            }

            Button("Quit") {
                NSApplication.shared.terminate(nil)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
        }
        .padding(12)
        .frame(width: 200)
    }
}
