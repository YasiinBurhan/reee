package top.niunaijun.blackbox.core.system.pm.installer;

import java.io.File;
import java.io.IOException;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageSettings;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.NativeUtils;

public interface Executor {
    String TAG = "InstallExecutor";

    int exec(BPackageSettings ps, InstallOption option, int userId);

    class CopyExecutor implements Executor {
        @Override
        public int exec(BPackageSettings ps, InstallOption option, int userId) {
            try {
                if (!option.isFlag(InstallOption.FLAG_SYSTEM)) {
                    NativeUtils.copyNativeLib(new File(ps.pkg.baseCodePath), BEnvironment.getAppLibDir(ps.pkg.packageName));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            if (option.isFlag(InstallOption.FLAG_STORAGE)) {
                File origFile = new File(ps.pkg.baseCodePath);
                File newFile = BEnvironment.getBaseApkDir(ps.pkg.packageName);
                try {
                    if (option.isFlag(InstallOption.FLAG_URI_FILE)) {
                        boolean b = FileUtils.renameTo(origFile, newFile);
                        if (!b) {
                            FileUtils.copyFile(origFile, newFile);
                        }
                    } else {
                        FileUtils.copyFile(origFile, newFile);
                    }
                    newFile.setReadOnly();
                    ps.pkg.baseCodePath = newFile.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            } else if (option.isFlag(InstallOption.FLAG_SYSTEM)) {
                
            }
            return 0;
        }
    }

    class CreatePackageExecutor implements Executor {
        @Override
        public int exec(BPackageSettings ps, InstallOption option, int userId) {
            FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
            FileUtils.mkdirs(BEnvironment.getAppDir(ps.pkg.packageName));
            FileUtils.mkdirs(BEnvironment.getAppLibDir(ps.pkg.packageName));
            return 0;
        }
    }

    class CreateUserExecutor implements Executor {
        @Override
        public int exec(BPackageSettings ps, InstallOption option, int userId) {
            String packageName = ps.pkg.packageName;
            FileUtils.deleteDir(BEnvironment.getDataLibDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataCacheDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataFilesDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataDatabasesDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDeDataDir(packageName, userId));
            return 0;
        }
    }

    class RemoveAppExecutor implements Executor {
        @Override
        public int exec(BPackageSettings ps, InstallOption option, int userId) {
            FileUtils.deleteDir(BEnvironment.getAppDir(ps.pkg.packageName));
            return 0;
        }
    }

    class RemoveUserExecutor implements Executor {
        @Override
        public int exec(BPackageSettings ps, InstallOption option, int userId) {
            String packageName = ps.pkg.packageName;
            FileUtils.deleteDir(BEnvironment.getDataDir(packageName, userId));
            FileUtils.deleteDir(BEnvironment.getDeDataDir(packageName, userId));
            FileUtils.deleteDir(BEnvironment.getExternalDataDir(packageName, userId));
            return 0;
        }
    }
}
