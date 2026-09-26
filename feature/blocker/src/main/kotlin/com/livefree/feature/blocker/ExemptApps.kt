package com.livefree.feature.blocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import com.livefree.core.model.EssentialApps
import com.livefree.core.model.EssentialReason

/**
 * Apps that are never blocked, even in allow-list mode, so the phone keeps working
 * and you can always get back to Live Free to unlock. Combines the well-known list
 * with this phone's actual defaults.
 */
object ExemptApps {

    fun load(context: Context): Map<String, EssentialReason> {
        val result = linkedMapOf(context.packageName to EssentialReason.THIS_APP)
        fun add(packageName: String?, reason: EssentialReason) {
            if (!packageName.isNullOrEmpty()) result.putIfAbsent(packageName, reason)
        }
        val pm = context.packageManager

        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            ?.let { add(it, EssentialReason.PHONE) }
        add(Telephony.Sms.getDefaultSmsPackage(context), EssentialReason.MESSAGES)
        packagesFor(pm, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            .forEach { add(it, EssentialReason.HOME_SCREEN) }
        packagesFor(pm, Intent(AlarmClock.ACTION_SHOW_ALARMS))
            .forEach { add(it, EssentialReason.ALARMS) }
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.showIntent?.creatorPackage
            ?.let { add(it, EssentialReason.ALARMS) }
        packagesFor(pm, Intent(Intent.ACTION_VIEW).setType(ContactsContract.Contacts.CONTENT_TYPE))
            .forEach { add(it, EssentialReason.CONTACTS) }
        context.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList
            ?.forEach { add(it.packageName, EssentialReason.KEYBOARD) }
        context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.forEach { add(it.resolveInfo.serviceInfo.packageName, EssentialReason.ACCESSIBILITY) }
        EssentialApps.KNOWN.forEach { (packageName, reason) -> add(packageName, reason) }
        return result
    }

    private fun packagesFor(pm: PackageManager, intent: Intent): List<String> =
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
}
