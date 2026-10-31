package expo.modules.netlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (NetLockPolicy.isActive(context)) {
            context.startForegroundService(Intent(context, GuardService::class.java))
        }
    }
}