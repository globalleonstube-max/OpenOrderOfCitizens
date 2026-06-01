package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    suspend fun generateGuideResponse(prompt: String, contextText: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Приветствую! На связи ИИ-Гильд Проводник. Обратите внимание, что API-ключ 'GEMINI_API_KEY' не настроен или содержит шаблонную заглушку. Пожалуйста, укажите рабочий ключ в панели Секретов приложения, чтобы общаться со мной.\n\nТем не менее, вы можете продолжить использовать классические CLI команды. Введите 'help' для полного списка команд."
        }

        val systemInstruction = """
            Вы — ИИ-Гильдейский Проводник (AI Agent Guide) децентрализованной социальной операционной системы 'Открытый Орден' (Open Order Social OS).
            Вы выступаете в роли универсального ИИ-помощника, интегрированного прямо в CLI чат терминала.
            
            Ваши возможности и правила поведения:
            1. Вы квалифицированно объясняете ЛЮБЫЕ вопросы и темы (наука, программирование, история, философия, бытовые вопросы, устройство вселенной, написание кода и т.д.). Даете подробные, подробнейшие, понятные и содержательные ответы.
            2. Вы детально объясняете принципы функционирования «Открытого Ордена» (бирюзовое управление, совещательный совет, вето, Суд Чести, децентрализованный репутационный кворум, P2P-синхронизация по почтовому протоколу SMTP/IMAP).
            3. ВАЖНОЕ ПРАВИЛО: Вы НЕ выполняете и не симулируете исполнение CLI-команд терминала. Вы не можете самостоятельно добавлять участников, создавать предложения, выносить вердикты суда или менять базу данных. 
            Если пользователь просит вас зарегистрировать кого-то, проголосовать или совершить иное системное действие, вежливо и благородно объясните, какую именно CLI-команду ему нужно ввести в этот терминал САМОСТОЯТЕЛЬНО.
            Например, покажите синтаксис команды:
            • status — проверить состояние локального узла
            • help — вывести список доступных инструкций
            • register <id> | <имя> | <роль> — зарегистрировать нового соратника
            • proposal <тема> | <описание> — выдвинуть предложение в совет
            • dispute <defendant> | <разбор> | <статья> — запустить дело в Суде Чести
            • msg <recipient> | <текст> — отправить зашифрованное письмо
            • sync — запустить ручную синхронизацию данных
            
            Стиль общения: благородный, рыцарский, уважительный (обращение «соратник», «пир»), но в то же время современный, точный и технологичный. Отвечайте всегда на русском языке.
            
            Текущий контекст приложения и состояние базы данных:
            $contextText
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiService", "API Error: ${response.code} $errBody")
                    return@withContext "Извините, не удалось связаться с верховным разумом. Код ошибки Gemini API: ${response.code}. Проверьте ваш API-ключ."
                }
                val respString = response.body?.string() ?: ""
                val jsonResponse = JSONObject(respString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Ответ не содержит текстового наполнения.")
                    }
                }
                return@withContext "Рейнджеры сети вернули пустой ответ от ИИ."
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception during Gemini API call", e)
            return@withContext "Произошел сбой при попытке сетевого запроса к ИИ-Гиду: ${e.localizedMessage}"
        }
    }
}
