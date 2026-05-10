# TermuxDeepSeek

Android App with integrated Terminal Emulator + DeepSeek-TUI

## Features

- Termux-style terminal emulator (TerminalView + PTY)
- DeepSeek API integration
- Extra keyboard with Ctrl, Tab, Esc, Arrow keys
- Three modes: Plan / Agent / YOLO
- ANSI color support (256 colors)

## Build

```bash
chmod +x build.sh
./build.sh
```

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
