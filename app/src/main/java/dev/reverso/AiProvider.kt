package dev.reverso

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

internal enum class AiProvider(
    val label: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val protocol: Protocol,
) {
    OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions", "", Protocol.OPENAI_COMPATIBLE),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1/messages", "claude-haiku-4-5-20251001", Protocol.ANTHROPIC),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "", Protocol.OPENAI_COMPATIBLE),
    CUSTOM("OpenAI-compatible", "", "", Protocol.OPENAI_COMPATIBLE),
}

internal enum class Protocol { OPENAI_COMPATIBLE, ANTHROPIC }

internal data class AiProviderConfig(
    val provider: AiProvider,
    val endpoint: String,
    val model: String,
) {
    fun validationError(): String? {
        if (endpoint.isBlank()) return "Enter a valid HTTPS API endpoint"
        val parsed = runCatching { URI(endpoint.trim()) }.getOrNull()
            ?: return "Enter a valid HTTPS API endpoint"
        if (parsed.scheme != "https" || parsed.host.isNullOrBlank() || parsed.rawUserInfo != null || parsed.rawQuery != null || parsed.rawFragment != null) {
            return "The API endpoint must use HTTPS and cannot contain credentials, query parameters, or a fragment"
        }
        if (model.isBlank()) return "Enter a model name"
        return null
    }
}

internal data class AiMessage(val role: String, val content: String, val requestContext: String? = null) {
    fun requestContent(): String = requestContext?.let { "$content\n\n$it" } ?: content
}

internal object AiConfigRepository {
    private const val PREFS = "reverso-ai-config-v1"
    private fun endpointKey(provider: AiProvider) = "${provider.name.lowercase()}.endpoint"
    private fun modelKey(provider: AiProvider) = "${provider.name.lowercase()}.model"

    fun load(context: Context): AiProviderConfig? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val provider = runCatching { AiProvider.valueOf(prefs.getString("active_provider", "") ?: "") }.getOrNull() ?: return null
        return loadFor(context, provider)
    }

    fun loadFor(context: Context, provider: AiProvider): AiProviderConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AiProviderConfig(
            provider,
            prefs.getString(endpointKey(provider), provider.defaultEndpoint).orEmpty(),
            prefs.getString(modelKey(provider), provider.defaultModel).orEmpty(),
        )
    }

    fun save(context: Context, config: AiProviderConfig, apiKey: String) {
        require(config.validationError() == null) { config.validationError() ?: "Invalid provider configuration" }
        if (apiKey.isNotBlank()) SecureCredentialStore.put(context, config.provider, apiKey.trim())
        require(SecureCredentialStore.has(context, config.provider)) { "Add an API key for ${config.provider.label} before saving the provider" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("active_provider", config.provider.name)
            .putString(endpointKey(config.provider), config.endpoint.trim())
            .putString(modelKey(config.provider), config.model.trim())
            .apply()
    }

    fun clear(context: Context, provider: AiProvider) {
        SecureCredentialStore.remove(context, provider)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit().remove(endpointKey(provider)).remove(modelKey(provider))
        if (prefs.getString("active_provider", null) == provider.name) edit.remove("active_provider")
        edit.apply()
    }
}

/** Stores the provider key as AES-GCM ciphertext; the encryption key never leaves Android Keystore. */
internal object SecureCredentialStore {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val PREFS = "reverso-ai-secret-v1"
    private fun keyAlias(provider: AiProvider) = "reverso.ai.${provider.name.lowercase()}.credential.v1"
    private fun keyName(provider: AiProvider) = "api-key-aes-gcm-${provider.name.lowercase()}"
    private const val IV_BYTES = 12

    fun has(context: Context, provider: AiProvider): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(keyName(provider))

    fun put(context: Context, provider: AiProvider, secret: String) {
        require(secret.isNotBlank()) { "API key cannot be empty" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(provider))
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        val stored = cipher.iv + encrypted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(keyName(provider), Base64.encodeToString(stored, Base64.NO_WRAP))
            .apply()
    }

    fun get(context: Context, provider: AiProvider): String? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(keyName(provider), null) ?: return null
        val stored = Base64.decode(encoded, Base64.NO_WRAP)
        require(stored.size > IV_BYTES) { "Stored AI credential is invalid" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(provider), GCMParameterSpec(128, stored.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(stored.copyOfRange(IV_BYTES, stored.size)).toString(Charsets.UTF_8)
    }

    fun remove(context: Context, provider: AiProvider) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(keyName(provider)).apply()
    }

    private fun getOrCreateKey(provider: AiProvider): java.security.Key {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val alias = keyAlias(provider)
        store.getKey(alias, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}

internal object AiChatClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_RESPONSE_BYTES = 1_000_000
    private const val MAX_MESSAGE_CHARS = 24_000

    fun complete(config: AiProviderConfig, apiKey: String, messages: List<AiMessage>): String {
        config.validationError()?.let { throw IOException(it) }
        require(apiKey.isNotBlank()) { "Configure an API key in AI settings" }
        require(messages.all { it.requestContent().length <= MAX_MESSAGE_CHARS } && messages.sumOf { it.requestContent().length } <= 64_000) {
            "Chat context is too large; shorten the conversation or file and try again"
        }
        val connection = (URL(config.endpoint).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "Reverso-Android")
        }
        when (config.provider.protocol) {
            Protocol.OPENAI_COMPATIBLE -> {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                if (config.provider == AiProvider.OPENROUTER) {
                    connection.setRequestProperty("HTTP-Referer", "https://github.com/mryuvraj1/Reverso")
                    connection.setRequestProperty("X-Title", "Reverso IDE")
                }
            }
            Protocol.ANTHROPIC -> {
                connection.setRequestProperty("x-api-key", apiKey)
                connection.setRequestProperty("anthropic-version", "2023-06-01")
            }
        }
        return try {
            val body = requestBody(config, messages).toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("${config.provider.label} returned HTTP $code. Check the endpoint, model, key, and account quota.")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw IOException("AI response exceeded the 1 MB limit")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            parseResponse(config.provider, JSONObject(bytes.toString(Charsets.UTF_8)))
        } finally {
            connection.disconnect()
        }
    }

    private fun requestBody(config: AiProviderConfig, messages: List<AiMessage>): JSONObject = when (config.provider.protocol) {
        Protocol.OPENAI_COMPATIBLE -> JSONObject()
            .put("model", config.model)
            .put("stream", false)
            .put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", "You are Reverso's coding assistant. Be accurate, concise, and explain risky code changes."))
                messages.forEach { put(JSONObject().put("role", it.role).put("content", it.requestContent())) }
            })
        Protocol.ANTHROPIC -> JSONObject()
            .put("model", config.model)
            .put("max_tokens", 2048)
            .put("system", "You are Reverso's coding assistant. Be accurate, concise, and explain risky code changes.")
            .put("messages", JSONArray().apply {
                messages.filter { it.role != "system" }.forEach { put(JSONObject().put("role", it.role).put("content", it.requestContent())) }
            })
    }

    private fun parseResponse(provider: AiProvider, json: JSONObject): String {
        val content = when (provider.protocol) {
            Protocol.OPENAI_COMPATIBLE -> json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content").orEmpty()
            Protocol.ANTHROPIC -> json.optJSONArray("content")?.let { blocks ->
                (0 until blocks.length()).mapNotNull { blocks.optJSONObject(it)?.takeIf { b -> b.optString("type") == "text" }?.optString("text") }
                    .joinToString("\n")
            }.orEmpty()
        }.trim()
        if (content.isEmpty()) throw IOException("${provider.label} returned no text response")
        return content
    }
}
