package expo.modules.netlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.UserManager
import android.provider.Settings

object NetLockPolicy {
    private const val PREFS = "netlock"
    private const val KEY_HOST = "host"
    private const val KEY_ACTIVE = "active"
    private const val KEY_START = "request_start"
    private const val KEY_BOOT = "request_boot"

    const val DELAY_MS = 2L * 60 * 1000
    const val WINDOW_MS = 30L * 60 * 1000

    private val restrictions = listOf(
        UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
        UserManager.DISALLOW_CONFIG_VPN,
        UserManager.DISALLOW_CONFIG_DATE_TIME
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dpm(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)

    private fun admin(context: Context) =
        ComponentName(context, NetLockAdminReceiver::class.java)

    private fun bootCount(context: Context) =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun isOwner(context: Context) = dpm(context).isDeviceOwnerApp(context.packageName)

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

    fun status(context: Context): Map<String, Any> = mapOf(
        "isOwner" to isOwner(context),
        "active" to isActive(context),
        "host" to host(context),
        "requested" to isRequested(context),
        "waitMs" to waitMs(context),
        "canDisable" to canDisable(context)
    )

    fun configure(context: Context, host: String) {
        check(!isActive(context)) { "Configuração bloqueada enquanto a proteção está ativa" }
        prefs(context).edit().putString(KEY_HOST, host.trim()).apply()
    }

    fun enable(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) error("Requer Android 11 ou superior")
        check(isOwner(context)) { "O app ainda não é Device Owner" }
        val host = host(context)
        require(host.isNotEmpty()) { "Defina o host do DNS nas configurações" }

        val manager = dpm(context)
        val admin = admin(context)

        manager.setAutoTimeEnabled(admin, true)
        val result = manager.setGlobalPrivateDnsModeSpecifiedHost(admin, host)
        check(result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) { "Falha ao aplicar o DNS (código $result)" }

        restrictions.forEach { manager.addUserRestriction(admin, it) }
        manager.setUninstallBlocked(admin, context.packageName, true)
        manager.setUserControlDisabledPackages(admin, listOf(context.packageName))

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) error("Requer Android 11 ou superior")
        check(canDisable(context)) { "Aguardando o período de liberação" }

        val manager = dpm(context)
        val admin = admin(context)

        restrictions.forEach { manager.clearUserRestriction(admin, it) }
        manager.setUninstallBlocked(admin, context.packageName, false)
        manager.setUserControlDisabledPackages(admin, emptyList())
        manager.setGlobalPrivateDnsModeOpportunistic(admin)

        clearRequest(context)
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply()
    }
}