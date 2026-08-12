package top.niunaijun.blackbox.app.configuration

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle

open class AppLifecycleCallback : Application.ActivityLifecycleCallbacks {

    open fun beforeMainLaunchApk(packageName: String?, userid: Int) {}

    open fun onStoragePermissionNeeded(packageName: String?, userId: Int): Boolean {
        return false
    }

    open fun beforeMainApplicationAttach(app: Application?, context: Context?) {}

    open fun afterMainApplicationAttach(app: Application?, context: Context?) {}

    open fun beforeMainActivityOnCreate(activity: Activity?) {}

    open fun afterMainActivityOnCreate(activity: Activity?) {}

    open fun beforeCreateApplication(packageName: String?, processName: String?, context: Context?, userId: Int) {}

    open fun beforeApplicationOnCreate(packageName: String?, processName: String?, application: Application?, userId: Int) {}

    open fun afterApplicationOnCreate(packageName: String?, processName: String?, application: Application?, userId: Int) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        @JvmField
        val EMPTY: AppLifecycleCallback = object : AppLifecycleCallback() {}
    }
}
