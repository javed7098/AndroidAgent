package com.example.androidaiagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 40, 30, 40)

        val title = TextView(this)
        title.text = "🤖 Android AI Agent"
        title.textSize = 26f

        val info = TextView(this)
        info.text = "\nAccessibility Service فعال ہونی چاہیے۔"

        val enable = Button(this)
        enable.text = "Enable Accessibility"
        enable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val home = Button(this)
        home.text = "🏠 Test Home"
        home.setOnClickListener {
            AgentAccessibilityService.instance?.goHome()
        }

        val back = Button(this)
        back.text = "◀ Test Back"
        back.setOnClickListener {
            AgentAccessibilityService.instance?.goBack()
        }

        val recents = Button(this)
        recents.text = "▣ Test Recents"
        recents.setOnClickListener {
            AgentAccessibilityService.instance?.openRecents()
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(enable)
        layout.addView(home)
        layout.addView(back)
        layout.addView(recents)

        setContentView(layout)
    }
}
