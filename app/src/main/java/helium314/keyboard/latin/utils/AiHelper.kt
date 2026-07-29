// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

object AiHelper {
    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    fun groqRequest(text: String, systemPrompt: String, token: String): String {
        val body = JSONObject().apply {
            put("model", "llama-3.1-8b-instant")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", text) })
            })
            put("temperature", 0.3)
            put("max_tokens", 1024)
        }
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val response = conn.inputStream.bufferedReader().readText()
        return JSONObject(response).optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")?.trim() ?: ""
    }

    fun groqRequestStreaming(text: String, systemPrompt: String, token: String, onChunk: (String) -> Unit): String {
        val body = JSONObject().apply {
            put("model", "llama-3.1-8b-instant")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", text) })
            })
            put("temperature", 0.3)
            put("max_tokens", 1024)
            put("stream", true)
        }
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val fullText = StringBuilder()
        val error = AtomicReference<String?>()
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (!trimmed.startsWith("data: ")) continue
                val data = trimmed.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val json = JSONObject(data)
                    val delta = json.optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) {
                        fullText.append(content)
                        onChunk(fullText.toString())
                    }
                } catch (e: Exception) {
                    error.compareAndSet(null, e.message)
                    break
                }
            }
        }
        val streamError = error.get()
        if (streamError != null) throw RuntimeException("Streaming error: $streamError")
        return fullText.toString().trim()
    }
}
