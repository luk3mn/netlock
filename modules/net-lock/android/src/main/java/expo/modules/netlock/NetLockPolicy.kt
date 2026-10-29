package expo.modules.netlock

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings

object NetLockPolicy {
    private const val PREFS = "netlock"
    private const val KEY_HOST = "host"
    private const val KEY_ACTIVE = "active"
    private const val KEY_START = "request_start"
    private const val KEY_BOOT = "request_boot"
    private const val DNS_MODE = "private_dns_mode"
    private const val DNS_SPECIFIER = "private_dns_specifier"

    const val DELAY_MS = 2L * 60 * 1000
    const val WINDOW_MS = 30L * 60 * 1000

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bootCount(context: Context) =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun canWriteSecureSettings(context: Context) =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isAdmin(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)
            .isAdminActive(ComponentName(context, NetLockAdminReceiver::class.java))

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val service = ComponentName(context, NetLockAccessibilityService::class.java)
        return enabled.split(':').any {
            it.equals(service.flattenToString(), ignoreCase = true) ||
                it.equals(service.flattenToShortString(), ignoreCase = true)
        }
    }

    fun host(context: Context): String = prefs(context).getString(KEY_HOST, "") ?: ""

    fun isActive(context: Context) = prefs(context).getBoolean(KEY_ACTIVE, false)

    private fun clearRequest(context: Context) {
        prefs(context).edit().remove(KEY_START).remove(KEY_BOOT).apply()
    }

    private fun elapsedSinceRequest(context: Context): Long? {
        val start = prefs(context).getLong(KEY_START, -1L)
        if (start < 0) return null
        val elapsed = SystemClock.elapsedRealtime() - start
        val sameBoot = prefs(context).getInt(KEY_BOOT, -1) == bootCount(context)
        if (!sameBoot || elapsed < 0 || elapsed > DELAY_MS + WINDOW_MS) {
            clearRequest(context)
            return null
        }
        return elapsed
    }

    fun isRequested(context: Context) = elapsedSinceRequest(context) != null

    fun waitMs(context: Context): Long =
        elapsedSinceRequest(context)?.let { (DELAY_MS - it).coerceAtLeast(0L) } ?: 0L

    fun canDisable(context: Context): Boolean =
        elapsedSinceRequest(context)?.let { it >= DELAY_MS } ?: false

    fun isDnsApplied(context: Context, host: String): Boolean {
        val resolver = context.contentResolver
        return Settings.Global.getString(resolver, DNS_MODE) == "hostname" &&
            Settings.Global.getString(resolver, DNS_SPECIFIER) == host
    }

    fun applyDns(context: Context, host: String) {
        val resolver = context.contentResolver
        Settings.Global.putString(resolver, DNS_SPECIFIER, host)
        Settings.Global.putString(resolver, DNS_MODE, "hostname")
    }

    private fun clearDns(context: Context) {
        Settings.Global.putString(context.contentResolver, DNS_MODE, "opportunistic")
    }

    fun status(context: Context): Map<String, Any> {
        val secure = canWriteSecureSettings(context)
        val admin = isAdmin(context)
        val accessibility = isAccessibilityEnabled(context)
        return mapOf(
            "secureSettings" to secure,
            "admin" to admin,
            "accessibility" to accessibility,
            "ready" to (secure && admin && accessibility),
            "active" to isActive(context),
            "host" to host(context),
            "requested" to isRequested(context),
            "waitMs" to waitMs(context),
            "canDisable" to canDisable(context)
        )
    }

    fun configure(context: Context, host: String) {
        check(!isActive(context)) { "Configuração bloqueada enquanto a proteção está ativa" }
        prefs(context).edit().putString(KEY_HOST, host.trim()).apply()
    }

    fun enable(context: Context) {
        check(canWriteSecureSettings(context)) { "Falta a permissão WRITE_SECURE_SETTINGS. Rode o comando adb." }
        check(isAdmin(context)) { "Ative o administrador do dispositivo" }
        check(isAccessibilityEnabled(context)) { "Ative o serviço de acessibilidade" }
        val host = host(context)
        require(host.isNotEmpty()) { "Defina o host do DNS nas configurações" }

        applyDns(context, host)
        clearRequest(context)
        prefs(context).edit().putBoolean(KEY_ACTIVE, true).apply()
    }

    fun requestDisable(context: Context) {
        check(isActive(context)) { "A proteção não está ativa" }
        if (isRequested(context)) return
        prefs(context).edit()
            .putLong(KEY_START, SystemClock.elapsedRealtime())
            .putInt(KEY_BOOT, bootCount(context))
            .apply()
    }

    fun cancelDisable(context: Context) = clearRequest(context)

    fun disable(context: Context) {
        check(canDisable(context)) { "Aguardando o período de liberação" }
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply()
        clearRequest(context)
        clearDns(context)
    }
}