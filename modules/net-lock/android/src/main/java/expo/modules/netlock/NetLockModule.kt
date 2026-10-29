package expo.modules.netlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class NetLockModule : Module() {
    private val context: Context
        get() = requireNotNull(appContext.reactContext)

    override fun definition() = ModuleDefinition {
        Name("NetLock")

        Function("status") { NetLockPolicy.status(context) }
        Function("configure") { host: String -> NetLockPolicy.configure(context, host) }
        Function("enable") { NetLockPolicy.enable(context) }
        Function("requestDisable") { NetLockPolicy.requestDisable(context) }
        Function("cancelDisable") { NetLockPolicy.cancelDisable(context) }
        Function("disable") { NetLockPolicy.disable(context) }

        Function("requestAdmin") {
            val activity = requireNotNull(appContext.currentActivity) { "Activity indisponível" }
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(context, NetLockAdminReceiver::class.java)
                )
            activity.startActivity(intent)
        }

        Function("openAccessibilitySettings") {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}