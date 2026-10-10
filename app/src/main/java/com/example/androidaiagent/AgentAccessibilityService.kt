package com.example.androidaiagent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AgentAccessibilityService? = null
            private set
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun voiceScrollUp(): Boolean = scroll(forward = false)
    fun voiceScrollDown(): Boolean = scroll(forward = true)
    fun voiceClickText(target: String): Boolean = clickText(target)
    fun voiceTypeText(value: String): Boolean = typeText(value)


    private var server: ServerSocket? = null
    private var serverRunning = false

    private val executor = Executors.newCachedThreadPool()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        startLocalServer()

        println("ANDROID AI AGENT: Accessibility connected")
        println("ANDROID AI AGENT: Local server = 127.0.0.1:8765")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {

        serverRunning = false

        try {
            server?.close()
        } catch (_: Exception) {
        }

        executor.shutdownNow()

        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun startLocalServer() {

        if (serverRunning) return

        serverRunning = true

        executor.execute {

            try {

                server = ServerSocket(8765)

                while (serverRunning) {

                    val client = server?.accept()

                    if (client != null) {
                        executor.execute {
                            handleClient(client)
                        }
                    }
                }

            } catch (e: Exception) {

                if (serverRunning) {
                    println(
                        "AI SERVER ERROR: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {

        socket.use {

            try {

                val reader =
                    BufferedReader(
                        InputStreamReader(
                            socket.getInputStream()
                        )
                    )

                val requestLine =
                    reader.readLine() ?: return

                val headers =
                    mutableMapOf<String, String>()

                while (true) {

                    val line =
                        reader.readLine() ?: break

                    if (line.isEmpty()) break

                    val index = line.indexOf(":")

                    if (index > 0) {

                        headers[
                            line.substring(0, index).trim()
                        ] =
                            line.substring(index + 1).trim()
                    }
                }

                val parts =
                    requestLine.split(" ")

                val method =
                    parts.getOrNull(0) ?: "GET"

                val path =
                    parts.getOrNull(1) ?: "/"

                val contentLength =
                    headers["Content-Length"]
                        ?.toIntOrNull()
                        ?: 0

                val bodyChars =
                    CharArray(contentLength)

                if (contentLength > 0) {
                    reader.read(bodyChars)
                }

                val body =
                    String(bodyChars)

                val response =
                    when {

                        method == "GET" &&
                        path.startsWith("/health") ->
                            JSONObject()
                                .put("ok", true)
                                .put(
                                    "service",
                                    "Android AI Agent"
                                )
                                .toString()

                        method == "GET" &&
                        path.startsWith("/screen") ->
                            screenJson()

                        method == "POST" &&
                        path.startsWith("/action") ->
                            executeJson(body)

                        else ->
                            JSONObject()
                                .put(
                                    "error",
                                    "Unknown endpoint"
                                )
                                .toString()
                    }

                sendResponse(
                    socket,
                    response
                )

            } catch (e: Exception) {

                try {

                    sendResponse(
                        socket,
                        JSONObject()
                            .put(
                                "error",
                                e.message ?: "error"
                            )
                            .toString()
                    )

                } catch (_: Exception) {
                }
            }
        }
    }

    private fun sendResponse(
        socket: Socket,
        body: String
    ) {

        val bytes =
            body.toByteArray(Charsets.UTF_8)

        val output: OutputStream =
            socket.getOutputStream()

        val header =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"

        output.write(
            header.toByteArray(Charsets.UTF_8)
        )

        output.write(bytes)
        output.flush()
    }

    private fun screenJson(): String {

        val result =
            JSONObject()

        val root =
            rootInActiveWindow

        if (root == null) {

            result.put(
                "error",
                "No active accessibility window"
            )

            result.put(
                "nodes",
                JSONArray()
            )

            return result.toString()
        }

        result.put(
            "package",
            root.packageName?.toString() ?: ""
        )

        val nodes =
            JSONArray()

        collectNodes(
            root,
            nodes,
            0
        )

        result.put(
            "nodes",
            nodes
        )

        return result.toString()
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        array: JSONArray,
        depth: Int
    ) {

        if (depth > 25) return

        val item =
            JSONObject()

        val bounds =
            Rect()

        node.getBoundsInScreen(bounds)

        item.put(
            "text",
            node.text?.toString() ?: ""
        )

        item.put(
            "description",
            node.contentDescription?.toString() ?: ""
        )

        item.put(
            "class",
            node.className?.toString() ?: ""
        )

        item.put(
            "id",
            node.viewIdResourceName ?: ""
        )

        item.put(
            "clickable",
            node.isClickable
        )

        item.put(
            "editable",
            node.isEditable
        )

        item.put(
            "enabled",
            node.isEnabled
        )

        item.put(
            "scrollable",
            node.isScrollable
        )

        item.put(
            "focused",
            node.isFocused
        )

        item.put(
            "visible",
            node.isVisibleToUser
        )

        item.put(
            "x1",
            bounds.left
        )

        item.put(
            "y1",
            bounds.top
        )

        item.put(
            "x2",
            bounds.right
        )

        item.put(
            "y2",
            bounds.bottom
        )

        item.put(
            "depth",
            depth
        )

        array.put(item)

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)

            if (child != null) {

                collectNodes(
                    child,
                    array,
                    depth + 1
                )
            }
        }
    }

    private fun executeJson(body: String): String {

        val result =
            JSONObject()

        try {

            val command =
                JSONObject(body)

            val action =
                command.optString(
                    "action"
                ).lowercase()

            val success =
                when (action) {

                    "open_app" -> {
                        val packageName = command.optString("package").trim()
                        if (packageName.isEmpty()) {
                            false
                        } else {
                            val launchIntent =
                                packageManager.getLaunchIntentForPackage(packageName)

                            if (launchIntent == null) {
                                false
                            } else {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                                true
                            }
                        }
                    }

                    "back" ->
                        performGlobalAction(
                            GLOBAL_ACTION_BACK
                        )

                    "home" ->
                        performGlobalAction(
                            GLOBAL_ACTION_HOME
                        )

                    "recents" ->
                        performGlobalAction(
                            GLOBAL_ACTION_RECENTS
                        )

                    "notifications" ->
                        performGlobalAction(
                            GLOBAL_ACTION_NOTIFICATIONS
                        )

                    "quick_settings" ->
                        performGlobalAction(
                            GLOBAL_ACTION_QUICK_SETTINGS
                        )

                    "power_dialog" ->
                        performGlobalAction(
                            GLOBAL_ACTION_POWER_DIALOG
                        )

                    "lock" ->
                        performGlobalAction(
                            GLOBAL_ACTION_LOCK_SCREEN
                        )

                    "screenshot" ->
                        performGlobalAction(
                            GLOBAL_ACTION_TAKE_SCREENSHOT
                        )

                    "tap" ->
                        tap(
                            command.optDouble("x").toFloat(),
                            command.optDouble("y").toFloat()
                        )

                    "long_press" ->
                        longPress(
                            command.optDouble("x").toFloat(),
                            command.optDouble("y").toFloat(),
                            command.optLong(
                                "duration",
                                1000
                            )
                        )

                    "swipe" ->
                        swipe(
                            command.optDouble("x1").toFloat(),
                            command.optDouble("y1").toFloat(),
                            command.optDouble("x2").toFloat(),
                            command.optDouble("y2").toFloat(),
                            command.optLong(
                                "duration",
                                500
                            )
                        )

                    "scroll_up" ->
                        scroll(
                            forward = true
                        )

                    "scroll_down" ->
                        scroll(
                            forward = false
                        )

                    "click_text" ->
                        clickText(
                            command.optString(
                                "text"
                            )
                        )

                    "type" ->
                        typeText(
                            command.optString(
                                "text"
                            )
                        )

                    "clear" ->
                        typeText("")

                    else -> false
                }

            result.put(
                "ok",
                success
            )

            result.put(
                "action",
                action
            )

        } catch (e: Exception) {

            result.put(
                "ok",
                false
            )

            result.put(
                "error",
                e.message ?: "invalid command"
            )
        }

        return result.toString()
    }

    private fun tap(
        x: Float,
        y: Float
    ): Boolean {

        val path =
            Path()

        path.moveTo(x, y)

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        80
                    )
                )
                .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun longPress(
        x: Float,
        y: Float,
        duration: Long
    ): Boolean {

        val path =
            Path()

        path.moveTo(x, y)

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration.coerceIn(
                            500,
                            5000
                        )
                    )
                )
                .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long
    ): Boolean {

        val path =
            Path()

        path.moveTo(x1, y1)
        path.lineTo(x2, y2)

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration.coerceIn(
                            100,
                            5000
                        )
                    )
                )
                .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    private fun scroll(
        forward: Boolean
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val node =
            findScrollable(root)

        if (node != null) {

            val action =
                if (forward) {
                    AccessibilityNodeInfo
                        .ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo
                        .ACTION_SCROLL_BACKWARD
                }

            return node.performAction(action)
        }

        return if (forward) {

            swipe(
                540f,
                1500f,
                540f,
                500f,
                500
            )

        } else {

            swipe(
                540f,
                500f,
                540f,
                1500f,
                500
            )
        }
    }

    private fun findScrollable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (node.isScrollable) {
            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)

            if (child != null) {

                val found =
                    findScrollable(child)

                if (found != null) {
                    return found
                }
            }
        }

        return null
    }

    private fun clickText(
        text: String
    ): Boolean {

        if (text.isBlank()) {
            return false
        }

        val root =
            rootInActiveWindow
                ?: return false

        val nodes =
            root.findAccessibilityNodeInfosByText(
                text
            )

        for (node in nodes) {

            if (node.isClickable) {

                if (
                    node.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                ) {
                    return true
                }
            }

            var parent =
                node.parent

            while (parent != null) {

                if (parent.isClickable) {

                    if (
                        parent.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK
                        )
                    ) {
                        return true
                    }

                    break
                }

                parent =
                    parent.parent
            }
        }

        return false
    }

    private fun typeText(
        text: String
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val focused =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )
                ?: return false

        val bundle =
            Bundle()

        bundle.putCharSequence(
            AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )

        return focused.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            bundle
        )
    }
}
