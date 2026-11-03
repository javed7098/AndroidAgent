package com.spider.ai

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        @JavascriptInterface
        fun scan() {
            if (running) return
            running = true
            executor.execute {
                try {
                    val profiles = getJson(
                        "https://api.dexscreener.com/token-profiles/latest/v1"
                    )
                    val output = JSONArray()
                    var count = 0

                    for (i in 0 until profiles.length()) {
                        if (count >= 25) break
                        val token = profiles.optJSONObject(i) ?: continue
                        if (token.optString("chainId") != "solana") continue
                        val address = token.optString("tokenAddress")
                        if (address.isBlank()) continue

                        try {
                            val pairs = getJson(
                                "https://api.dexscreener.com/token-pairs/v1/solana/$address"
                            )
                            for (j in 0 until pairs.length()) {
                                if (count >= 25) break
                                val p = pairs.optJSONObject(j) ?: continue
                                val liq = p.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0
                                val vol = p.optJSONObject("volume")?.optDouble("h24", 0.0) ?: 0.0
                                val change = p.optJSONObject("priceChange")?.optDouble("h24", 0.0) ?: 0.0
                                val fdv = p.optDouble("fdv", 0.0)
                                val flags = JSONArray()

                                if (liq < 5000) flags.put("LOW LIQUIDITY")
                                if (liq > 0 && fdv / liq > 20) flags.put("HIGH FDV/LIQ")
                                if (liq > 0 && vol / liq > 5) flags.put("UNUSUAL VOLUME")
                                if (kotlin.math.abs(change) > 100) flags.put("HIGH VOLATILITY")

                                val base = p.optJSONObject("baseToken") ?: JSONObject()
                                val item = JSONObject()
                                item.put("name", base.optString("name", "Unknown"))
                                item.put("symbol", base.optString("symbol", "?"))
                                item.put("liquidity", liq)
                                item.put("volume", vol)
                                item.put("change", change)
                                item.put("fdv", fdv)
                                item.put("flags", flags)
                                item.put("pair", p.optString("pairAddress"))
                                output.put(item)
                                count++
                            }
                        } catch (_: Exception) {}
                    }
                    web.post {
                        web.evaluateJavascript(
                            "showResults(${JSONObject.quote(output.toString())})", null
                        )
                    }
                } catch (e: Exception) {
                    web.post {
                        web.evaluateJavascript(
                            "showError(${JSONObject.quote(e.message ?: "Network error")})", null
                        )
                    }
                } finally {
                    running = false
                }
            }
        }

        @JavascriptInterface
        fun stop() {
            running = false
        }
    }

    private fun getJson(address: String): JSONArray {
        val c = URL(address).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "SpiderAI/1.0")
        return try {
            JSONArray(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        web.destroy()
        super.onDestroy()
    }
}
