# Keep Xposed entry point
-keep class Akari.Twitter.share.hook.MainHook { *; }

# Keep module status check
-keep class Akari.Twitter.share.util.ModuleStatus { *; }

# Keep ContentProvider
-keep class Akari.Twitter.share.provider.ConfigProvider { *; }