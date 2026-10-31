package Akari.Twitter.share.util

/**
 * Utility to check if this module is active in LSPosed/Xposed.
 * MainHook hooks this method in the module's own package to return true.
 */
object ModuleStatus {
    @JvmStatic
    fun isModuleActive(): Boolean {
        return false
    }
}