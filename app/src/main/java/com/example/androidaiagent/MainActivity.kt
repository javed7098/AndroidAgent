package com.example.androidaiagent

import android.app.Activity
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import java.util.Locale
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : Activity() {

    private lateinit var apiKey: EditText
    private lateinit var question: EditText
    private lateinit var answer: TextView
    private lateinit var sendButton: Button
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 30, 24, 24)

        val title = TextView(this)
        title.text = "🤖 Android AI Agent"
        title.textSize = 25f
        layout.addView(title)

        val info = TextView(this)
        info.text = "\nChatGPT Cloud Assistant\n"
        info.textSize = 18f
        layout.addView(info)

        apiKey = EditText(this)
        apiKey.hint = "OpenAI API Key یہاں درج کریں"
        apiKey.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(apiKey)

        val note = TextView(this)
        note.text = "API Key کسی کو نہ بھیجیں۔ یہ ایپ اسے محفوظ نہیں کرے گی۔\n"
        layout.addView(note)

        question = EditText(this)
        question.hint = "اپنا سوال یہاں لکھیں"
        question.minLines = 2
        question.maxLines = 5
        question.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        layout.addView(question)

        val voiceButton = Button(this)
        voiceButton.text = "🎤 اردو میں بولیں"
        voiceButton.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    7102
                )
            }
        }
        layout.addView(voiceButton)

        sendButton = Button(this)
        sendButton.text = "☁️ ChatGPT سے جواب لیں"
        layout.addView(sendButton)

        answer = TextView(this)
        answer.text = "\nCloud کا جواب یہاں ظاہر ہوگا۔\n"
        answer.textSize = 17f
        layout.addView(answer)

        val enable = Button(this)
        enable.text = "Enable Accessibility"
        enable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        layout.addView(enable)

        val home = Button(this)
        home.text = "🏠 Test Home"
        home.setOnClickListener {
            AgentAccessibilityService.instance?.goHome()
                ?: run { answer.text = "Accessibility سروس منسلک نہیں ہے۔" }
        }
        layout.addView(home)

        val back = Button(this)
        back.text = "◀ Test Back"
        back.setOnClickListener {
            AgentAccessibilityService.instance?.goBack()
                ?: run { answer.text = "Accessibility سروس منسلک نہیں ہے۔" }
        }
        layout.addView(back)

        val recents = Button(this)
        recents.text = "▣ Test Recents"
        recents.setOnClickListener {
            AgentAccessibilityService.instance?.openRecents()
                ?: run { answer.text = "Accessibility سروس منسلک نہیں ہے۔" }
        }
        layout.addView(recents)

        sendButton.setOnClickListener {
            sendToCloud()
        }

        val scroll = ScrollView(this)
        scroll.addView(
            layout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)
    }


    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "اردو میں کمانڈ بولیں")
            startActivityForResult(intent, 7101)
        } catch (e: Exception) {
            answer.text = "آواز پہچاننے کی سہولت دستیاب نہیں: ${e.message}"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7102) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput()
            } else {
                answer.text = "آواز کے لیے مائیکروفون کی اجازت ضروری ہے۔"
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 7101 || resultCode != RESULT_OK) return

        val spokenText = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()

        if (spokenText.isNullOrEmpty()) {
            answer.text = "آواز سمجھ نہیں آئی۔ دوبارہ بولیں۔"
            return
        }

        question.setText(spokenText)
        answer.text = "آپ نے کہا: $spokenText"

        if (!runLocalCommand(spokenText)) {
            answer.append("\nیہ متن سوال والے خانے میں رکھ دیا ہے۔ ChatGPT کے لیے بٹن دبائیں۔")
        }
    }

    private fun runLocalCommand(rawText: String): Boolean {
        val text = rawText.lowercase(Locale.ROOT).trim()
        val service = AgentAccessibilityService.instance

        when {
            text in listOf("home", "go home", "ہوم", "گھر", "ہوم کھولو", "گھر جاؤ") -> {
                if (service != null) service.goHome()
                else answer.text = "Accessibility سروس منسلک نہیں ہے۔"
                return true
            }

            text in listOf("back", "go back", "واپس", "بیک", "پیچھے جاؤ") -> {
                if (service != null) service.goBack()
                else answer.text = "Accessibility سروس منسلک نہیں ہے۔"
                return true
            }

            text in listOf("recents", "recent apps", "حالیہ ایپس", "ریسنٹس") -> {
                if (service != null) service.openRecents()
                else answer.text = "Accessibility سروس منسلک نہیں ہے۔"
                return true
            }

            text.contains("نیچے اسکرول") || text == "scroll down" -> {
                val ok = service?.voiceScrollDown() ?: false
                answer.text = if (ok) "اسکرین نیچے کی جا رہی ہے۔" else "اسکرول نہیں ہو سکا۔"
                return true
            }

            text.contains("اوپر اسکرول") || text == "scroll up" -> {
                val ok = service?.voiceScrollUp() ?: false
                answer.text = if (ok) "اسکرین اوپر کی جا رہی ہے۔" else "اسکرول نہیں ہو سکا۔"
                return true
            }

            text.startsWith("کلک ") || text.startsWith("click ") -> {
                val target = rawText.trim().substringAfter(" ").trim()
                val ok = target.isNotBlank() && (service?.voiceClickText(target) ?: false)
                answer.text = if (ok) "کلک کر دیا گیا۔" else "بٹن نہیں ملا۔"
                return true
            }

            text.startsWith("لکھو ") || text.startsWith("type ") -> {
                val target = rawText.trim().substringAfter(" ").trim()
                val ok = target.isNotBlank() && (service?.voiceTypeText(target) ?: false)
                answer.text = if (ok) "متن لکھ دیا گیا۔" else "پہلے ٹیکسٹ باکس منتخب کریں۔"
                return true
            }

            text in listOf("google", "open google", "گوگل", "گوگل کھولو", "گوگل اوپن کرو", "گوگل کھولیں") -> {
                return launchGoogle()
            }

            text.contains("whatsapp") || text.contains("واٹس ایپ") -> {
                return launchApp("com.whatsapp", "WhatsApp")
            }

            text.contains("youtube") || text.contains("یوٹیوب") -> {
                return launchApp("com.google.android.youtube", "YouTube")
            }

            text.contains("chrome") || text.contains("کروم") -> {
                return launchApp("com.android.chrome", "Chrome")
            }
        }

        return false
    }

    private fun launchGoogle(): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                ?: packageManager.getLaunchIntentForPackage("com.google.android.apps.googleassistant")

            if (intent == null) {
                answer.text = "Google ایپ فون میں نہیں ملی۔"
            } else {
                startActivity(intent)
                answer.text = "Google کھولی جا رہی ہے۔"
            }
            true
        } catch (e: Exception) {
            answer.text = "Google نہیں کھل سکی: ${e.message}"
            true
        }
    }

    private fun launchApp(packageName: String, appName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent == null) {
                answer.text = "$appName ایپ فون میں نہیں ملی۔"
            } else {
                startActivity(intent)
                answer.text = "$appName کھولی جا رہی ہے۔"
            }
            true
        } catch (e: Exception) {
            answer.text = "$appName نہیں کھل سکی: ${e.message}"
            true
        }
    }

    private fun sendToCloud() {
        val key = apiKey.text.toString().trim()
        val prompt = question.text.toString().trim()

        if (key.isEmpty()) {
            answer.text = "پہلے OpenAI API Key درج کریں۔"
            return
        }

        if (prompt.isEmpty()) {
            answer.text = "پہلے اپنا سوال لکھیں۔"
            return
        }

        sendButton.isEnabled = false
        answer.text = "☁️ ChatGPT Cloud سے رابطہ ہو رہا ہے..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL("https://api.openai.com/v1/responses")
                    .openConnection() as HttpURLConnection)

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Authorization", "Bearer $key"
                )
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )

                val body = JSONObject()
                    .put("model", "gpt-4.1-mini")
                    .put("input", prompt)
                    .put("store", false)

                OutputStreamWriter(
                    connection.outputStream, Charsets.UTF_8
                ).use { writer ->
                    writer.write(body.toString())
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val result = BufferedReader(
                    InputStreamReader(stream, Charsets.UTF_8)
                ).use { it.readText() }

                val json = JSONObject(result)

                if (status !in 200..299) {
                    val message = json.optJSONObject("error")
                        ?.optString("message")
                        ?: "HTTP $status"
                    throw Exception(message)
                }

                val output = json.optJSONArray("output")
                val text = StringBuilder()

                if (output != null) {
                    for (i in 0 until output.length()) {
                        val item = output.optJSONObject(i) ?: continue
                        if (item.optString("type") != "message") continue

                        val content = item.optJSONArray("content") ?: continue
                        for (j in 0 until content.length()) {
                            val part = content.optJSONObject(j) ?: continue
                            if (part.optString("type") == "output_text") {
                                text.append(part.optString("text"))
                            }
                        }
                    }
                }

                val finalAnswer = text.toString().ifBlank {
                    "Cloud سے جواب نہیں ملا۔ دوبارہ کوشش کریں۔"
                }

                handler.post {
                    answer.text = "🤖 ChatGPT کا جواب:\n\n$finalAnswer"
                }

            } catch (e: Exception) {
                val message = e.message ?: "نامعلوم خرابی"
                handler.post {
                    answer.text =
                        "❌ Cloud رابطہ ناکام ہوا:\n$message\n\n" +
                        "انٹرنیٹ، API Key اور API اکاؤنٹ چیک کریں۔"
                }
            } finally {
                connection?.disconnect()
                handler.post {
                    sendButton.isEnabled = true
                }
            }
        }.start()
    }
}
