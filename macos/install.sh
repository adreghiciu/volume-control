#!/bin/bash
set -e

echo "Installing VolumeControl..."

# Copy app to Applications folder
cp -r VolumeControl.app /Applications/

echo "App installed to /Applications/VolumeControl.app"

# Install LaunchAgent for auto-start at login
mkdir -p ~/Library/LaunchAgents

cat > ~/Library/LaunchAgents/com.volumecontrol.app.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>Label</key>
	<string>com.volumecontrol.app</string>
	<key>ProgramArguments</key>
	<array>
		<string>/Applications/VolumeControl.app/Contents/MacOS/VolumeControl</string>
	</array>
	<key>RunAtLoad</key>
	<true/>
	<key>KeepAlive</key>
	<false/>
</dict>
</plist>
EOF

echo "LaunchAgent plist created at ~/Library/LaunchAgents/com.volumecontrol.app.plist"

# Load the LaunchAgent
launchctl load ~/Library/LaunchAgents/com.volumecontrol.app.plist || launchctl unload ~/Library/LaunchAgents/com.volumecontrol.app.plist && launchctl load ~/Library/LaunchAgents/com.volumecontrol.app.plist

echo "Installation complete!"
echo "The app will start automatically on login and is running now."
echo ""
echo "To test the HTTP API:"
echo "  curl http://localhost:8888/"
echo "  curl -X POST http://localhost:8888/ -H 'Content-Type: application/json' -d '{\"volume\": 50}'"
echo "  curl -X POST http://localhost:8888/ -H 'Content-Type: application/json' -d '{\"muted\": true}'"
echo "  curl -X POST http://localhost:8888/ -H 'Content-Type: application/json' -d '{\"volume\": 75, \"muted\": false}'"
