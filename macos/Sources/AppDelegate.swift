import AppKit
import SwiftUI

class AppDelegate: NSObject, NSApplicationDelegate {
    var statusItem: NSStatusItem?
    var popover: NSPopover?
    var volumeController: VolumeController?
    var httpServer: HTTPServer?

    func applicationDidFinishLaunching(_ notification: Notification) {
        volumeController = VolumeController()
        httpServer = HTTPServer(volumeController: volumeController!)

        do {
            try httpServer?.start()
            print("HTTP server started on port 8888")
        } catch {
            print("Failed to start HTTP server: \(error)")
        }

        setupStatusBarItem()
    }

    func setupStatusBarItem() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)

        if let button = statusItem?.button {
            button.title = "🔊"
            button.action = #selector(togglePopover)
            button.target = self
        }

        let volumeView = VolumeView(volumeController: volumeController!)
        let hostingView = NSHostingView(rootView: volumeView)

        popover = NSPopover()
        popover?.contentViewController = NSViewController()
        popover?.contentViewController?.view = hostingView
        popover?.contentViewController?.view.frame.size = CGSize(width: 224, height: 140)
        popover?.appearance = NSAppearance(named: .aqua)
    }

    @objc func togglePopover() {
        guard let popover = popover, let button = statusItem?.button else { return }

        if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return false
    }
}
