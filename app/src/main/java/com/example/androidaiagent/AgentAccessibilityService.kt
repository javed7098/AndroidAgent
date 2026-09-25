package com.example.androidaiagent

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)

        for (node in nodes) {
            if (node.isClickable &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
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
