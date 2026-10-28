package expo.modules.netlock

import android.content.Context
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
        Function("removeOwner") { NetLockPolicy.removeOwner(context) }
    }
}