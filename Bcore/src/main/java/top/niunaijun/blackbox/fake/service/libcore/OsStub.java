package top.niunaijun.blackbox.fake.service.libcore;

import android.os.Process;

import java.lang.reflect.Method;

import black.libcore.io.BRLibcore;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.IOCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Reflector;


public class OsStub extends ClassInvocationStub {
    public static final String TAG = "OsStub";
    private Object mBase;

    public OsStub() {
        mBase = BRLibcore.get().os();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRLibcore.get()._set_os(proxyInvocation);
    }

    @Override
    protected void onBindMethod() {
    }

    @Override
    public boolean isBadEnv() {
        return BRLibcore.get().os() != getProxyInvocation();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null)
                    continue;
                if (args[i] instanceof String && ((String) args[i]).startsWith("/")) {
                    String orig = (String) args[i];
                    args[i] = IOCore.get().redirectPath(orig);



                }
            }
        }
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("getuid")
    public static class getuid extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int callUid = (int) method.invoke(who, args);
            return getFakeUid(callUid);
        }
    }

    @ProxyMethod("stat")
    public static class stat extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object invoke = null;
            try {
                invoke = method.invoke(who, args);
            } catch (Throwable e) {
                throw e.getCause();
            }
            Reflector.with(invoke).field("st_uid").set(getFakeUid(-1));
            return invoke;
        }
    }

    private static int getFakeUid(int callUid) {
        if (callUid > 0 && callUid <= Process.FIRST_APPLICATION_UID)
            return callUid;

        if (BActivityThread.isThreadInit() && BActivityThread.currentActivityThread().isInit()) {
            return BActivityThread.getBAppId();
        } else {
            return BlackBoxCore.getHostUid();
        }
    }

    private static String getAbsolutePathWithManualDotResolve(String path) {
        if (path == null) {
            return null;
        }
        String absPath = new java.io.File(path).getAbsolutePath();
        String[] parts = absPath.split("/");
        java.util.Stack<String> stack = new java.util.Stack<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            } else {
                stack.push(part);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String s : stack) {
            sb.append("/").append(s);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private static String safeNormalizePath(String path) {
        if (path == null) {
            return null;
        }
        try {
            return new java.io.File(path).getCanonicalPath();
        } catch (Exception e) {
            return getAbsolutePathWithManualDotResolve(path);
        }
    }

    private static boolean isVirtualPath(String path) {
        if (path == null) {
            return false;
        }
        String normalizedCandidate = safeNormalizePath(path);
        if (normalizedCandidate == null) {
            return false;
        }
        try {
            java.io.File virtualRootFile = top.niunaijun.blackbox.core.env.BEnvironment.getVirtualRoot();
            if (virtualRootFile != null) {
                String virtualRoot = safeNormalizePath(virtualRootFile.getAbsolutePath());
                if (virtualRoot != null) {
                    if (normalizedCandidate.equals(virtualRoot) || normalizedCandidate.startsWith(virtualRoot + java.io.File.separator)) {
                        return true;
                    }
                }
            }
            java.io.File externalVirtualRootFile = top.niunaijun.blackbox.core.env.BEnvironment.getExternalVirtualRoot();
            if (externalVirtualRootFile != null) {
                String externalVirtualRoot = safeNormalizePath(externalVirtualRootFile.getAbsolutePath());
                if (externalVirtualRoot != null) {
                    if (normalizedCandidate.equals(externalVirtualRoot) || normalizedCandidate.startsWith(externalVirtualRoot + java.io.File.separator)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "OsStub: Error during virtual path validation", e);
        }
        return false;
    }

    private static String getPathFromFd(Object[] args, int fdIndex) {
        if (args == null || args.length <= fdIndex || args[fdIndex] == null) {
            return null;
        }
        if (args[fdIndex] instanceof java.io.FileDescriptor) {
            java.io.FileDescriptor fd = (java.io.FileDescriptor) args[fdIndex];
            int fdVal = -1;
            try {
                java.lang.reflect.Field field = java.io.FileDescriptor.class.getDeclaredField("descriptor");
                field.setAccessible(true);
                fdVal = field.getInt(fd);
            } catch (NoSuchFieldException e1) {
                try {
                    java.lang.reflect.Field field = java.io.FileDescriptor.class.getDeclaredField("fd");
                    field.setAccessible(true);
                    fdVal = field.getInt(fd);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            } catch (IllegalAccessException ignored) {
            }

            if (fdVal >= 0) {
                try {
                    return android.system.Os.readlink("/proc/self/fd/" + fdVal);
                } catch (android.system.ErrnoException e) {
                    android.util.Log.w(TAG, "OsStub: Failed to readlink fd " + fdVal, e);
                }
            }
        }
        return null;
    }

    private static Object handleChownException(String methodName, Object[] args, String resolvedPath, Throwable t) throws Throwable {
        Throwable cause = t;
        if (t instanceof java.lang.reflect.InvocationTargetException) {
            cause = t.getCause();
        }
        if (cause == null) {
            cause = t;
        }
        if (cause instanceof android.system.ErrnoException) {
            android.system.ErrnoException errnoException = (android.system.ErrnoException) cause;
            if (errnoException.errno == android.system.OsConstants.EPERM) {
                if (isVirtualPath(resolvedPath)) {
                    android.util.Log.d(TAG, "OsStub: suppressed EPERM from " + methodName + " for virtual path: " + resolvedPath);
                    return null;
                }
            }
            android.util.Log.d(TAG, "OsStub: rethrowing filesystem error errno=" + errnoException.errno + " from " + methodName + " for path: " + resolvedPath);
        }
        throw cause;
    }

    @ProxyMethod("chown")
    public static class Chown extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String resolvedPath = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                resolvedPath = safeNormalizePath((String) args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return handleChownException("chown", args, resolvedPath, t);
            }
        }
    }

    @ProxyMethod("fchown")
    public static class Fchown extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String resolvedPath = getPathFromFd(args, 0);
            if (resolvedPath != null) {
                resolvedPath = safeNormalizePath(resolvedPath);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return handleChownException("fchown", args, resolvedPath, t);
            }
        }
    }

    @ProxyMethod("lchown")
    public static class Lchown extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String resolvedPath = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                resolvedPath = safeNormalizePath((String) args[0]);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return handleChownException("lchown", args, resolvedPath, t);
            }
        }
    }

    @ProxyMethod("fchownat")
    public static class Fchownat extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String resolvedPath = null;
            if (args != null && args.length > 1) {
                String pathname = null;
                if (args[1] instanceof String) {
                    pathname = (String) args[1];
                }
                if (pathname != null) {
                    if (pathname.startsWith("/")) {
                        resolvedPath = pathname;
                    } else {
                        String baseDir = getPathFromFd(args, 0);
                        if (baseDir != null) {
                            resolvedPath = baseDir + java.io.File.separator + pathname;
                        }
                    }
                }
            }
            if (resolvedPath != null) {
                resolvedPath = safeNormalizePath(resolvedPath);
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                return handleChownException("fchownat", args, resolvedPath, t);
            }
        }
    }
}
