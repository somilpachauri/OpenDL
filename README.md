# ⚡ OpenDL - Java Download Manager

OpenDL is a high-speed, multi-threaded download manager built entirely in Java. It features seamless Google Chrome integration via a custom local server, smart HTTP redirect chasing, and robust pause/resume capabilities. 

Built with clean architecture principles, it handles complex networking edge cases (like chunked transfer encoding and dynamic `Content-Disposition` headers) that standard downloaders fail to catch.

## ✨ Key Features
* **Multi-Threaded Acceleration:** Divides large files into multiple chunks and downloads them concurrently for maximum bandwidth utilization.
* **Chrome Extension Integration:** Intercepts browser downloads seamlessly using a custom JavaScript extension communicating with a Java local HTTP server (via IPC).
* **Smart Header Extraction:** Automatically sniffs `Content-Disposition` and `Content-Type` headers to correctly name dynamically generated files (e.g., from CDN or converter sites).
* **Robust Network Handling:** Automatically chases HTTP 301/302/307 redirects and elegantly falls back to single-threaded mode if a server blocks chunked encoding.
* **Pause & Resume:** Gracefully halts worker threads and saves progress, allowing downloads to be resumed at any time without data corruption.
* **Download History:** Zero-dependency JSON storage engine to track and manage past downloads.

## 📦 Installation (Windows)
You do not need Java installed on your computer to run OpenDL! It comes bundled as a standalone Windows application.

1. Go to the [Releases](../../releases) page of this repository.
2. Download the latest `OpenDL-1.0.exe` installer.
3. Run the installer and follow the setup wizard.
4. Launch OpenDL from your desktop or Start menu!
Note on Windows SmartScreen: > Because this is a free, open-source project, it does not have an expensive Code Signing Certificate. When you run the installer, Windows may show a blue "Windows protected your PC" popup.

To proceed, click More info.

Then click the Run anyway button that appears at the bottom.

## 🔌 Setting up the Chrome Extension
To get the true "1-click" download experience from your browser:
1. Open Google Chrome and navigate to `chrome://extensions/`.
2. Toggle **Developer mode** on (top right corner).
3. Click **Load unpacked** (top left corner).
4. Select the `OpenDL_Extension` folder located in this repository.
5. Make sure the OpenDL desktop app is running, and click any download link in Chrome!

## 🧩 Architecture Overview
OpenDL is designed using a clean, MVC-inspired architecture to separate UI threads from heavy network operations:
* `OpenDL.java`: The main Swing UI layer.
* `DownloadTask.java`: Orchestrates the worker threads, calculates speeds, and manages the UI listener callbacks.
* `DownloadWorker.java`: The individual `Runnable` threads handling `HttpURLConnection` byte-range requests.
* `NetworkUtils.java`: The "sniffer" that chases redirects and extracts precise file metadata before the download begins.
* `LocalServer.java`: A lightweight `com.sun.net.httpserver` listening on port `12345` for payloads from the Chrome extension.

## 👨‍💻 Author
**Somil Pachauri** * [View my Portfolio Website](https://somilpachauri.in) * GitHub: [@somilpachauri](https://github.com/somilpachauri)

---
*If you found this project helpful or interesting, feel free to give it a ⭐!*
