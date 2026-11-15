package com.universalremote.tv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVBrand
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small encrypted SharedPreferences store backed by an Android Keystore AES-GCM key. */
class CredentialStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tv_credentials", Context.MODE_PRIVATE)
    private val alias = "universal_tv_remote_aes"

    @Synchronized
    fun saveCredential(ip: String, key: String, value: String) {
        require(ip.isNotBlank() && key.isNotBlank())
        prefs.edit().putString("$ip::$key", encrypt(value)).apply()
    }

    @Synchronized
    fun getCredential(ip: String, key: String): String? = prefs.getString("$ip::$key", null)?.let { runCatching { decrypt(it) }.getOrNull() }

    @Synchronized
    fun saveTV(tv: DiscoveredTV) {
        val obj = JSONObject().apply {
            put("ip", tv.ip); put("brand", tv.brand.name); put("modelName", tv.modelName)
            put("friendlyName", tv.friendlyName); put("port", tv.port); put("protocol", tv.protocol)
            put("requiresPairing", tv.requiresPairing); put("macAddress", tv.macAddress ?: ""); put("descriptionUrl", tv.descriptionUrl ?: "")
            put("lastConnected", System.currentTimeMillis())
        }
        saveCredential(tv.ip, "device", obj.toString())
    }

    @Synchronized
    fun getAllSavedTVs(): List<DiscoveredTV> = prefs.all.keys.filter { it.endsWith("::device") }.mapNotNull { key ->
        val ip = key.substringBefore("::")
        getCredential(ip, "device")?.let { raw -> runCatching {
            val o = JSONObject(raw)
            DiscoveredTV(
                ip = o.optString("ip", ip),
                brand = runCatching { TVBrand.valueOf(o.optString("brand", "UNKNOWN")) }.getOrDefault(TVBrand.UNKNOWN),
                modelName = o.optString("modelName", ""),
                friendlyName = o.optString("friendlyName", "Smart TV"),
                port = o.optInt("port", 80),
                protocol = o.optString("protocol", "HTTP"),
                requiresPairing = o.optBoolean("requiresPairing", false),
                macAddress = o.optString("macAddress").takeIf { it.isNotBlank() },
                descriptionUrl = o.optString("descriptionUrl").takeIf { it.isNotBlank() }
            )
        }.getOrNull() }
    }

    @Synchronized
    fun deleteCredential(ip: String, key: String) {
        prefs.edit().remove("$ip::$key").apply()
    }

    @Synchronized
    fun deleteTVCredentials(ip: String) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$ip::") }.forEach(editor::remove)
        editor.apply()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > 12) { "Invalid encrypted value" }
        val iv = bytes.copyOfRange(0, 12)
        val payload = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }
}
