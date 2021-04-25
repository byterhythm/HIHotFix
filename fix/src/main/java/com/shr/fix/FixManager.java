package com.shr.fix;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class FixManager {
    private static HashSet<File> loadDex = new HashSet<>();
    private HashSet<Integer> fixVersion = new HashSet<>();

    private Context context;

    public FixManager(Context base) {
        this.context = base;
    }

    public boolean checkNeedFix() {
        boolean needFix = false;
        if (context != null) {
            int longVersionCode = 0;
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
                        PackageManager.GET_CONFIGURATIONS);
                longVersionCode = packageInfo.versionCode;
                //获取需要热修复的版本
                fixVersion.add(1);
                for (Integer integer : fixVersion) {
                    if (integer == longVersionCode) {
                        needFix = true;
                        break;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return needFix;
    }

    public void loadDex( ) {
        if (context == null) return;
        //1.这里展示从网络已经下载到本地文件，读取该文件
        String dexName = "out.dex";
        File downLoadFile = new File(Environment.getExternalStorageDirectory(), dexName);
        //2.copy 该文件到应用程序私有目录/data/data/应用包名/xx
        File odex = context.getDir("odex", Context.MODE_PRIVATE);
        copyToPrivatePath(downLoadFile, new File(odex, dexName));
        //3.获取所有的补丁
        File[] files = odex.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("classes") || file.getName().endsWith(".dex")) {
                loadDex.add(file);
            }
        }
        //4.创建一个dex文件解压目录
        String optimizeDirectory = odex.getAbsolutePath() + File.separator + "opt_dex";
        File optFile = new File(optimizeDirectory);
        if (!optFile.exists()) {
            optFile.mkdirs();
        }
        for (File dex : loadDex) {
            //加载获取本地的dexElement[],首先拿到PathClassLoader
            try {
                PathClassLoader classLoader = (PathClassLoader) context.getClassLoader();
                Class<?> baseDexClassLoader = classLoader.getClass().getSuperclass();
                Field pathListField = baseDexClassLoader.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathListValue = pathListField.get(classLoader);
                Field dexElementsField = pathListValue.getClass().getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                //在这里拿到系统的dexElements
                Object dexElementsValue = dexElementsField.get(pathListValue);

                DexClassLoader dexClassLoader = new DexClassLoader(dex.getAbsolutePath(), optimizeDirectory, null, context.getClassLoader());
                //获取到了dexClassLoader 当中的pathList；
                Object myPathList = pathListField.get(dexClassLoader);
                Object myDexElements = dexElementsField.get(myPathList);

                //合并新的数据
                int length = Array.getLength(dexElementsValue);
                int myLength = Array.getLength(myDexElements);
                int newLength = length + myLength;
                //获取到数组的类型
                Class<?> componentType = dexElementsValue.getClass().getComponentType();
                Object newElementArray = Array.newInstance(componentType, newLength);
                for (int i = 0; i < newLength; i++) {
                    if (i < myLength) {
                        Array.set(newElementArray, i, Array.get(myDexElements, i));
                    } else {
                        Array.set(newElementArray, i, Array.get(dexElementsValue, i - myLength));
                    }
                }
                //set新的数据
                dexElementsField.set(pathListValue, newElementArray);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void copyToPrivatePath(File downloadFile, File targetFile) {

        InputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try {
            // 判断该文件是否存在，如果存在删除，再copy
            if (targetFile.exists()) {
                targetFile.delete();
            }
            inputStream = new FileInputStream(downloadFile);
            outputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
