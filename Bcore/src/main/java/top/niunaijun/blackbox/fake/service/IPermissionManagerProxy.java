package top.niunaijun.blackbox.fake.service;

import android.content.pm.PackageManager;
import android.Manifest;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import black.android.os.BRServiceManager;
import black.android.permission.BRIPermissionManagerStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.base.ValueMethodProxy;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class IPermissionManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IPermissionManagerProxy";

    private static final String P = "permissionmgr";

    public IPermissionManagerProxy() {
        super(BRServiceManager.get().getService(P));
    }

    @Override
    protected Object getWho() {
        return BRIPermissionManagerStub.get().asInterface(BRServiceManager.get().getService(P));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("permissionmgr");
        BRActivityThread.getWithException()._set_sPermissionManager(proxyInvocation);
        
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addPermissionAsync", true));
        addMethodHook(new ValueMethodProxy("addPermission", true));
        addMethodHook(new ValueMethodProxy("performDexOpt", true));
        addMethodHook(new ValueMethodProxy("performDexOptIfNeeded", false));
        addMethodHook(new ValueMethodProxy("performDexOptSecondary", true));
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("checkDeviceIdentifierAccess", false));
        addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
        if (BuildCompat.isOreo()) {
            addMethodHook(new ValueMethodProxy("notifyDexLoad", 0));
            addMethodHook(new ValueMethodProxy("notifyPackageUse", 0));
            addMethodHook(new ValueMethodProxy("setInstantAppCookie", false));
            addMethodHook(new ValueMethodProxy("isInstantApp", false));
        }
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permission = null;
            String packageName = null;
            for (Object arg : args) {
                if (arg instanceof String) {
                    String str = (String) arg;
                    if (str.contains("permission")) {
                        permission = str;
                    } else if (packageName == null) {
                        packageName = str;
                    }
                }
            }
            
            int uid = top.niunaijun.blackbox.app.BActivityThread.getAppConfig() != null ? top.niunaijun.blackbox.app.BActivityThread.getAppConfig().uid : -1;
            int userId = top.niunaijun.blackbox.app.BActivityThread.getAppConfig() != null ? top.niunaijun.blackbox.app.BActivityThread.getAppConfig().userId : 0;
            
            if (permission != null) {
                boolean manifestDeclared = false;
                if (packageName != null) {
                    manifestDeclared = isPermissionDeclaredInManifest(packageName, permission);
                }
                
                boolean finalResultGranted = manifestDeclared && isWhitelistPermission(permission);
                String virtualState = isWhitelistPermission(permission) ? "WHITELISTED" : "REGULAR";
                String finalResult = finalResultGranted ? "GRANTED" : "DENIED";
                
                Slog.d(TAG, String.format("PermissionAudit [pkg=%s, uid=%d, userId=%d, permission=%s, manifestDeclared=%b, virtualState=%s, finalResult=%s]",
                    packageName, uid, userId, permission, manifestDeclared, virtualState, finalResult));
                
                if (finalResultGranted) {
                    return PackageManager.PERMISSION_GRANTED;
                }
            }
            // Fallback to calling with host package name
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String && args[i].equals(packageName)) {
                    args[i] = top.niunaijun.blackbox.BlackBoxCore.getHostPkg();
                    break;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("checkUidPermission")
    public static class CheckUidPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int uid = -1;
            String permission = null;
            for (Object arg : args) {
                if (arg instanceof Integer) {
                    uid = (Integer) arg;
                } else if (arg instanceof String) {
                    String str = (String) arg;
                    if (str.contains("permission")) {
                        permission = str;
                    }
                }
            }
            
            String packageName = null;
            if (top.niunaijun.blackbox.app.BActivityThread.getAppConfig() != null && top.niunaijun.blackbox.app.BActivityThread.getAppConfig().uid == uid) {
                packageName = top.niunaijun.blackbox.app.BActivityThread.getAppConfig().packageName;
            }
            int userId = top.niunaijun.blackbox.app.BActivityThread.getAppConfig() != null ? top.niunaijun.blackbox.app.BActivityThread.getAppConfig().userId : 0;
            
            if (permission != null) {
                boolean manifestDeclared = false;
                if (packageName != null) {
                    manifestDeclared = isPermissionDeclaredInManifest(packageName, permission);
                }
                
                boolean finalResultGranted = manifestDeclared && isWhitelistPermission(permission);
                String virtualState = isWhitelistPermission(permission) ? "WHITELISTED" : "REGULAR";
                String finalResult = finalResultGranted ? "GRANTED" : "DENIED";
                
                Slog.d(TAG, String.format("PermissionAudit [pkg=%s, uid=%d, userId=%d, permission=%s, manifestDeclared=%b, virtualState=%s, finalResult=%s]",
                    packageName, uid, userId, permission, manifestDeclared, virtualState, finalResult));
                
                if (finalResultGranted) {
                    return PackageManager.PERMISSION_GRANTED;
                }
            }
            // Modify uid to host uid
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer && (Integer) args[i] == uid) {
                    args[i] = top.niunaijun.blackbox.BlackBoxCore.getHostUid();
                    break;
                }
            }
            return method.invoke(who, args);
        }
    }

    private static boolean isPermissionDeclaredInManifest(String packageName, String permission) {
        if (packageName == null || permission == null) return false;
        try {
            android.content.pm.PackageInfo packageInfo = top.niunaijun.blackbox.BlackBoxCore.getBPackageManager().getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS, top.niunaijun.blackbox.core.system.user.BUserHandle.getUserId(top.niunaijun.blackbox.app.BActivityThread.getBAppId()));
            if (packageInfo != null && packageInfo.requestedPermissions != null) {
                for (String reqPerm : packageInfo.requestedPermissions) {
                    if (reqPerm.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isWhitelistPermission(String permission) {
        if (permission == null) return false;
        return permission.equals(android.Manifest.permission.INTERNET) ||
               permission.equals(android.Manifest.permission.ACCESS_NETWORK_STATE) ||
               permission.equals(android.Manifest.permission.ACCESS_WIFI_STATE) ||
               isAudioPermission(permission) ||
               isStorageOrMediaPermission(permission) ||
               isNotificationOrXiaomiPermission(permission);
    }

    private static boolean isStorageOrMediaPermission(String permission) {
        if (permission == null) return false;
        if (permission.equals(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                || permission.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return true;
        }
        if (permission.equals(android.Manifest.permission.READ_MEDIA_AUDIO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_VIDEO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_IMAGES)
                || permission.equals("android.permission.READ_MEDIA_VISUAL")
                || permission.equals("android.permission.READ_MEDIA_AURAL")
                || permission.equals(android.Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            return true;
        }
        if (permission.equals("android.permission.READ_MEDIA_AUDIO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VIDEO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_IMAGES_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_AURAL_USER_SELECTED")) {
            return true;
        }
        return false;
    }

    private static boolean isAudioPermission(String permission) {
        if (permission == null) return false;
        return permission.equals(android.Manifest.permission.RECORD_AUDIO)
                || permission.equals(android.Manifest.permission.CAPTURE_AUDIO_OUTPUT)
                || permission.equals(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                || permission.equals("android.permission.FOREGROUND_SERVICE_MICROPHONE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CAMERA")
                || permission.equals("android.permission.FOREGROUND_SERVICE_LOCATION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_HEALTH")
                || permission.equals("android.permission.FOREGROUND_SERVICE_DATA_SYNC")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SPECIAL_USE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED")
                || permission.equals("android.permission.FOREGROUND_SERVICE_PHONE_CALL")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE");
    }

    private static boolean isNotificationOrXiaomiPermission(String permission) {
        if (permission == null) return false;
        if (permission.equals("android.permission.POST_NOTIFICATIONS")) {
            return true;
        }
        if (permission.equals("miui.permission.USE_INTERNAL_GENERAL_API") ||
            permission.equals("miui.permission.OPTIMIZE_POWER") ||
            permission.equals("miui.permission.RUN_IN_BACKGROUND") ||
            permission.equals("miui.permission.POST_NOTIFICATIONS") ||
            permission.equals("miui.permission.AUTO_START") ||
            permission.equals("miui.permission.SHOW_ON_LOCKSCREEN")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

}
