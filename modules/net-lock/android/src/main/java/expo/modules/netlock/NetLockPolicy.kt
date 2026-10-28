package expo.modules.netlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager

object NetLockPolicy {
    private const val PREFS = "netlock"
    private const val KEY_HOST = "host"
    private const val KEY_ACTIVE = "active"
    private const val KEY_UNLOCK_AT = "unlock_at"

    const val DELAY_MS = 2L * 60 * 1000
    const val WINDOW_MS = 30L * 60 * 1000

    private val restrictions = listOf(
        UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
        UserManager.DISALLOW_CONFIG_VPN,
        UserManager.DISALLOW_CONFIG_DATE_TIME,
        UserManager.DISALLOW_SAFE_BOOT
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dpm(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)

    private fun admin(context: Context) =
        ComponentName(context, NetLockAdminReceiver::class.java)

    fun isOwner(context: Context) = dpm(context).isDeviceOwnerApp(context.packageName)

    fun host(context: Context): String = prefs(context).getString(KEY_HOST, "") ?: ""

    fun isActive(context: Context) = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun unlockAt(context: Context): Long {
        val at = prefs(context).getLong(KEY_UNLOCK_AT, 0L)
        if (at != 0L && System.currentTimeMillis() > at + WINDOW_MS) {
            prefs(context).edit().putLong(KEY_UNLOCK_AT, 0L).apply()
            return 0L
        }
        return at
    }

    fun canDisable(context: Context): Boolean {
        val at = unlockAt(context)
        return at != 0L && System.currentTimeMillis() >= at
    }

    fun status(context: Context): Map<String, Any> = mapOf(
        "isOwner" to isOwner(context),
        "active" to isActive(context),
        "host" to host(context),
        "unlockAt" to unlockAt(context),
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

        prefs(context).edit().putBoolean(KEY_ACTIVE, true).putLong(KEY_UNLOCK_AT, 0L).apply()
    }

    fun requestDisable(context: Context): Long {
        check(isActive(context)) { "A proteção não está ativa" }
        if (unlockAt(context) == 0L) {
            prefs(context).edit()
                .putLong(KEY_UNLOCK_AT, System.currentTimeMillis() + DELAY_MS)
                .apply()
        }
        return unlockAt(context)
    }

    fun cancelDisable(context: Context) {
        prefs(context).edit().putLong(KEY_UNLOCK_AT, 0L).apply()
    }

    fun disable(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) error("Requer Android 11 ou superior")
        check(canDisable(context)) { "Aguardando o período de liberação" }

        val manager = dpm(context)
        val admin = admin(context)

        restrictions.forEach { manager.clearUserRestriction(admin, it) }
        manager.setUninstallBlocked(admin, context.packageName, false)
        manager.setUserControlDisabledPackages(admin, emptyList())
        manager.setGlobalPrivateDnsModeOpportunistic(admin)

        prefs(context).edit().putBoolean(KEY_ACTIVE, false).putLong(KEY_UNLOCK_AT, 0L).apply()
    }

    fun removeOwner(context: Context) {
        check(!isActive(context)) { "Desative a proteção primeiro" }
        dpm(context).clearDeviceOwnerApp(context.packageName)
    }
}