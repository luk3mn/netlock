package expo.modules.netlock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NetLockAccessibilityService : AccessibilityService() {
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = enforceDns()
    }

    private val label by lazy {
        packageManager.getApplicationLabel(applicationInfo).toString().lowercase()
    }

    private val sensitive by lazy {
        listOf(
            "desinstalar", "uninstall",
            "forçar parada", "force stop",
            "limpar dados", "clear data",
            "administrador", "administrator",
            "acessibilidade", "accessibility",
            "desativar", "deactivate",
            "parar", "stop",
            "usar $label", "use $label"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        listOf("private_dns_mode", "private_dns_specifier").forEach {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        enforceDns()
        if (NetLockPolicy.isActive(this)) {
            runCatching { startForegroundService(Intent(this, GuardService::class.java)) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!NetLockPolicy.isActive(this) || NetLockPolicy.canDisable(this)) return
        val texts = (rootInActiveWindow ?: return).collectText().toList()
        val mentionsApp = texts.any { it.contains(label) }
        val sensitiveScreen = texts.any { text -> sensitive.any { text.contains(it) } }
        if (mentionsApp && sensitiveScreen) performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun enforceDns() {
        if (!NetLockPolicy.isActive(this)) return
        NetLockPolicy.ensureDns(this)
    }

    private fun AccessibilityNodeInfo.collectText(): Sequence<String> = sequence {
        text?.let { yield(it.toString().lowercase()) }
        contentDescription?.let { yield(it.toString().lowercase()) }
        for (i in 0 until childCount) {
            getChild(i)?.let { yieldAll(it.collectText()) }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }
}