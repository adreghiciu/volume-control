import Foundation
import Network

class HTTPServer {
    let port: NWEndpoint.Port = 8888
    var listener: NWListener?
    var volumeController: VolumeController

    init(volumeController: VolumeController) {
        self.volumeController = volumeController
    }

    func start() throws {
        listener = try NWListener(using: .tcp, on: port)

        listener?.newConnectionHandler = { [weak self] connection in
            self?.handleConnection(connection)
        }

        listener?.stateUpdateHandler = { state in
            switch state {
            case .ready:
                print("HTTP server listening on port 8888")
            case .failed(let error):
                print("HTTP server error: \(error)")
            default:
                break
            }
        }

        listener?.start(queue: DispatchQueue(label: "http-server-queue"))
    }

    func stop() {
        listener?.cancel()
    }

    private func handleConnection(_ connection: NWConnection) {
        connection.stateUpdateHandler = { [weak self] state in
            switch state {
            case .ready:
                self?.receiveData(on: connection)
            case .failed, .cancelled:
                connection.cancel()
            default:
                break
            }
        }

        connection.start(queue: DispatchQueue(label: "connection-queue"))
    }

    private func receiveData(on connection: NWConnection) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) { [weak self] data, _, isComplete, error in
            if let data = data, !data.isEmpty {
                if let request = String(data: data, encoding: .utf8) {
                    let response = self?.handleRequest(request) ?? self?.notFoundResponse() ?? ""
                    let responseData = response.data(using: .utf8) ?? Data()
                    connection.send(content: responseData, completion: .contentProcessed({ error in
                        if error != nil {
                            print("Error sending response: \(String(describing: error))")
                        }
                        connection.cancel()
                    }))
                }
            } else if isComplete || error != nil {
                connection.cancel()
            }
        }
    }

    private func handleRequest(_ request: String) -> String? {
        let lines = request.split(separator: "\n", maxSplits: 1)
        guard let requestLine = lines.first else { return nil }

        let components = requestLine.split(separator: " ")
        guard components.count >= 2 else { return nil }

        let method = String(components[0])
        let path = String(components[1])

        if method == "GET" && (path == "/" || path.isEmpty) {
            return getStatusResponse()
        } else if method == "POST" && (path == "/" || path.isEmpty) {
            if let bodyStart = request.range(of: "\r\n\r\n") ?? request.range(of: "\n\n") {
                let body = String(request[bodyStart.upperBound...])
                return setStatusResponse(body: body)
            }
            return badRequestResponse()
        }

        return notFoundResponse()
    }

    private func getStatusResponse() -> String {
        let volume = volumeController.volume
        let muted = volumeController.muted
        let body = "{\"volume\": \(volume), \"muted\": \(muted)}"
        return httpResponse(statusCode: 200, body: body)
    }

    private func setStatusResponse(body: String) -> String {
        if let jsonData = body.data(using: .utf8),
           let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {

            if let volume = json["volume"] as? NSNumber {
                volumeController.setVolume(volume.intValue)
            }

            if let muted = json["muted"] as? NSNumber {
                volumeController.setMuted(muted.boolValue)
            }

            let responseBody = "{\"volume\": \(volumeController.volume), \"muted\": \(volumeController.muted)}"
            return httpResponse(statusCode: 200, body: responseBody)
        }
        return badRequestResponse()
    }

    private func badRequestResponse() -> String {
        return httpResponse(statusCode: 400, body: "{\"error\": \"Bad request\"}")
    }

    private func notFoundResponse() -> String {
        return httpResponse(statusCode: 404, body: "{\"error\": \"Not found\"}")
    }

    private func httpResponse(statusCode: Int, body: String) -> String {
        let statusText: String
        switch statusCode {
        case 200:
            statusText = "OK"
        case 400:
            statusText = "Bad Request"
        case 404:
            statusText = "Not Found"
        default:
            statusText = "Unknown"
        }

        let bodyWithNewline = body + "\n"
        let responseHeaders = "HTTP/1.1 \(statusCode) \(statusText)\r\nContent-Type: application/json\r\nContent-Length: \(bodyWithNewline.count)\r\nConnection: close\r\n\r\n"

        return responseHeaders + bodyWithNewline
    }
}
