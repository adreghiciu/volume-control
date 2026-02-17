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

        listener?.stateUpdateHandler = { [weak self] state in
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
                    connection.send(content: responseData, completion: .idempotent)
                }
            }

            if isComplete || error != nil {
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

        if method == "GET" && path == "/volume" {
            return getVolumeResponse()
        } else if method == "POST" && path == "/volume" {
            if let bodyStart = request.range(of: "\r\n\r\n") ?? request.range(of: "\n\n") {
                let body = String(request[bodyStart.upperBound...])
                return setVolumeResponse(body: body)
            }
            return badRequestResponse()
        }

        return notFoundResponse()
    }

    private func getVolumeResponse() -> String {
        let volume = volumeController.volume
        let body = "{\"volume\": \(volume)}"
        return httpResponse(statusCode: 200, body: body)
    }

    private func setVolumeResponse(body: String) -> String {
        if let jsonData = body.data(using: .utf8),
           let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any],
           let volume = json["volume"] as? NSNumber {
            volumeController.setVolume(volume.intValue)
            let responseBody = "{\"volume\": \(volumeController.volume)}"
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

        let responseHeaders = """
        HTTP/1.1 \(statusCode) \(statusText)\r
        Content-Type: application/json\r
        Content-Length: \(body.count)\r
        Connection: close\r
        \r
        """

        return responseHeaders + body
    }
}
