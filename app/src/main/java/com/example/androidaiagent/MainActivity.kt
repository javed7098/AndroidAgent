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
        info.text = "\nAccessibility Service کو فعال کریں تاکہ Agent screen کو پڑھ اور user-authorized actions کر سکے۔"

        val button = Button(this)
        button.text = "Enable Accessibility"

        button.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(button)

        setContentView(layout)
    }
}
