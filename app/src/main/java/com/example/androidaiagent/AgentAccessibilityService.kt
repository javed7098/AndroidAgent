package com.example.androidaiagent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AgentAccessibilityService? = null
    }

    private val commandReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.getStringExtra("command")?.uppercase()) {
                "HOME" -> goHome()
                "BACK" -> goBack()
                "RECENTS" -> openRecents()
                "NOTIFICATIONS" -> openNotifications()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val filter = android.content.IntentFilter("com.example.androidaiagent.COMMAND")

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                commandReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(commandReceiver)
        } catch (_: Exception) {
        }

        if (instance === this) {
            instance = null
        }

        super.onDestroy()
    }

    fun goHome(): Boolean =
        performGlobalAction(GLOBAL_ACTION_HOME)

    fun goBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    fun openRecents(): Boolean =
        performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun openNotifications(): Boolean =
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)

        for (node in nodes) {
            try {
                if (node.isClickable &&
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            } finally {
                node.recycle()
            }
        }

        return false
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return ""
        return readNode(root)
    }

    private fun readNode(node: AccessibilityNodeInfo): String {
        val result = StringBuilder()

        node.text?.let {
            result.append(it).append("\n")
        }

        node.contentDescription?.let {
            result.append(it).append("\n")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                result.append(readNode(child))
                child.recycle()
            }
        }

        return result.toString()
    }
}
