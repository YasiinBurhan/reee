package top.niunaijun.blackbox.fake.service;

import android.util.Log;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;


public class ResourcesManagerProxy extends ClassInvocationStub {
    public static final String TAG = "ResourcesManagerProxy";

    private static final String RESOURCES_MANAGER_CLASS = "android.app.ResourcesManager";

    public ResourcesManagerProxy() {
        try {
            Class.forName(RESOURCES_MANAGER_CLASS);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "ResourcesManager class not found: " + e.getMessage());
        }
    }

    @Override
    protected Object getWho() {
        return null; 
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
    }

    @ProxyMethod("loadApkAssets")
    public static class LoadApkAssets extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Log.w(TAG, String.format("Resource_Audit [caller=%s, path=%s, action=LoadApkAssets, error=%s]",
                        who != null ? who.getClass().getName() : "null", path, t.getMessage()));
                throw t;
            }
        }
    }

    @ProxyMethod("loadOverlayFromPath")
    public static class LoadOverlayFromPath extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String path = (String) args[0];
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Log.w(TAG, String.format("Resource_Audit [caller=%s, path=%s, action=LoadOverlayFromPath, error=%s]",
                        who != null ? who.getClass().getName() : "null", path, t.getMessage()));
                throw t;
            }
        }
    }
}
