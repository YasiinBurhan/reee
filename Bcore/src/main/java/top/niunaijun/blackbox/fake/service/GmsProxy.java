package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    private final IBinder mCustomBinder;
    private final Object mCustomInterface;

    public GmsProxy() {
        super(BRServiceManager.get().getService("gms"));
        this.mCustomBinder = null;
        this.mCustomInterface = null;
    }

    public GmsProxy(IBinder customBinder, Object customInterface) {
        super(customBinder);
        this.mCustomBinder = customBinder;
        this.mCustomInterface = customInterface;
    }

    @Override
    protected Object getWho() {
        if (mCustomInterface != null) {
            return mCustomInterface;
        }
        IBinder binder = mCustomBinder != null ? mCustomBinder : BRServiceManager.get().getService("gms");
        if (binder == null) {
            Slog.d(TAG, "GMS service binder not present on device");
            return null;
        }
        
        ClassLoader classLoader = null;
        try {
            if (BActivityThread.getApplication() != null) {
                classLoader = BActivityThread.getApplication().getClassLoader();
            }
        } catch (Throwable ignored) {}
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = GmsProxy.class.getClassLoader();
        }

        try {
            Class<?> stubClass = null;
            try {
                stubClass = Class.forName("com.google.android.gms.common.internal.IGmsServiceBroker$Stub", true, classLoader);
            } catch (ClassNotFoundException e) {
                try {
                    stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub", true, classLoader);
                } catch (ClassNotFoundException e2) {
                    // Try to find the interface as fallback
                }
            }

            if (stubClass != null) {
                try {
                    Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                    Object iface = asInterfaceMethod.invoke(null, binder);
                    if (iface != null) {
                        Slog.d(TAG, "Successfully obtained IGmsServiceBroker interface using " + stubClass.getName());
                        return iface;
                    }
                } catch (Throwable t) {
                    Slog.w(TAG, "Failed to call asInterface on Gms Stub, trying fallback: " + t.getMessage());
                }
            }

            // Fallback to dynamic proxy of the interface if stub is missing
            Class<?> gmsInterface = null;
            try {
                gmsInterface = Class.forName("com.google.android.gms.common.internal.IGmsServiceBroker", true, classLoader);
            } catch (ClassNotFoundException e) {
                try {
                    gmsInterface = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker", true, classLoader);
                } catch (ClassNotFoundException e2) {
                    Slog.e(TAG, "Neither GMS stub nor interface found");
                }
            }

            if (gmsInterface != null) {
                final Class<?> finalGmsInterface = gmsInterface;
                final IBinder finalBinder = binder;
                Object proxyInstance = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[] { finalGmsInterface, android.os.IInterface.class },
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("asBinder".equals(method.getName())) {
                                return finalBinder;
                            }
                            try {
                                return method.invoke(finalBinder, args);
                            } catch (Throwable t) {
                                Slog.w(TAG, "Dynamic proxy fallback invoked method failed: " + method.getName());
                                return null;
                            }
                        }
                    }
                );
                Slog.d(TAG, "GmsProxy: Created custom fallback dynamic proxy for " + finalGmsInterface.getName());
                return proxyInstance;
            }
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IGmsServiceBroker interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (mCustomBinder == null) {
            replaceSystemService("gms");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    public static IBinder createProxy(IBinder binder) {
        if (binder == null) return null;
        if (binder instanceof BinderInvocationStub) return binder;
        try {
            GmsProxy gmsProxy = new GmsProxy(binder, null);
            gmsProxy.injectHook();
            Object proxy = gmsProxy.getProxyInvocation();
            if (proxy != null) {
                Slog.d(TAG, "Successfully created proxy for bound GMS service");
                return gmsProxy;
            }
        } catch (Throwable t) {
            Slog.e(TAG, "Failed to create GMS proxy", t);
        }
        return binder;
    }

    
    private static void fixGmsFields(Object obj, String hostPkg) {
        if (obj == null) return;
        String virtualPkg = BlackBoxCore.getAppPackageName();
        if (obj instanceof android.os.Bundle) {
            try {
                android.os.Bundle bundle = (android.os.Bundle) obj;
                for (String key : new java.util.HashSet<>(bundle.keySet())) {
                    Object val = bundle.get(key);
                    if (val instanceof String) {
                        String strVal = (String) val;
                        if ("com.google.android.gms".equals(strVal) || (virtualPkg != null && virtualPkg.equals(strVal))) {
                            bundle.putString(key, hostPkg);
                        }
                    }
                }
            } catch (Throwable ignored) {}
            return;
        }
        if (obj instanceof String) {
            return; // Strings are handled directly in the hook loop
        }
        Class<?> clazz = obj.getClass();
        while (clazz != null && !clazz.getName().startsWith("java.lang.")) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    if (field.getType() == String.class) {
                        String val = (String) field.get(obj);
                        if ("com.google.android.gms".equals(val) || (virtualPkg != null && virtualPkg.equals(val))) {
                            field.set(obj, hostPkg);
                            Slog.d(TAG, "GmsProxy: Fixed field " + field.getName() + " in " + clazz.getSimpleName() + " to " + hostPkg);
                        }
                    } else if (android.os.Bundle.class.isAssignableFrom(field.getType())) {
                        Object bundleVal = field.get(obj);
                        if (bundleVal != null) {
                            fixGmsFields(bundleVal, hostPkg);
                        }
                    }
                } catch (Throwable ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (!BlackBoxCore.get().isBlackProcess()) {
                    return method.invoke(who, args);
                }
                String hostPkg = BlackBoxCore.getHostPkg();
                String virtualPkg = BlackBoxCore.getAppPackageName();
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof String) {
                            String argStr = (String) args[i];
                            if ("com.google.android.gms".equals(argStr) || (virtualPkg != null && virtualPkg.equals(argStr))) {
                                args[i] = hostPkg;
                                Slog.d(TAG, "GmsProxy: Fixed calling package string at index " + i + " to " + hostPkg);
                            }
                        } else if (args[i] != null && !(args[i] instanceof Number) && !(args[i] instanceof Boolean)) {
                            fixGmsFields(args[i], hostPkg);
                        }
                    }
                }
                return method.invoke(who, args);
            } catch (Throwable e) {
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    Throwable cause = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
                    Slog.w(TAG, "GmsProxy SecurityException intercepted in getService: " + cause.getMessage());
                    return null;
                }
                Slog.w(TAG, "GmsProxy: Intercepted getService error safely: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (!BlackBoxCore.get().isBlackProcess()) {
                    return method.invoke(who, args);
                }
                String hostPkg = BlackBoxCore.getHostPkg();
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            fixGmsFields(args[i], hostPkg);
                        }
                    }
                }
                return method.invoke(who, args);
            } catch (Throwable e) {
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    Throwable cause = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
                    Slog.w(TAG, "GmsProxy SecurityException intercepted in getServiceBroker: " + cause.getMessage());
                    return null;
                }
                Slog.w(TAG, "GmsProxy: Error in getServiceBroker", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Authentication error, returning success", e);
                
                return createMockAuthResult();
            }
        }
    }

    
    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    
    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    
    private static Object createMockAuthResult() {
        try {
            
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
