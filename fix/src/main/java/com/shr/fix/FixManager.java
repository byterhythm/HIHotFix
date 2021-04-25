package com.shr.fix;

import android.content.Context;
import android.os.Environment;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class FixManager {
    String dexName = "out.dex";
    private String optimizeDirectory;

    private Set<File> loadDex = new HashSet<>();

    private File downLoad(String dexUrl) {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + dexName);
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    public void loadDex(Context context) {
        //1.这里展示从网络已经下载到本地文件，读取该文件
        File downLoadFile = downLoad("xxx");
        //2.copy 该文件到应用程序私有目录/data/data/应用包名/xx
        File targetFile = context.getDir("odex", Context.MODE_PRIVATE);
        optimizeDirectory = targetFile.getAbsolutePath() + File.separator + "opt_dex";
        File optFile = new File(optimizeDirectory);
        if (!optFile.exists()) {
            optFile.mkdirs();
        }
        copyToPrivatePath(downLoadFile, new File(targetFile, dexName));

        for (File file : targetFile.listFiles()) {
            if (file.getName().startsWith("classes") || file.getName().endsWith(".dex")) {
                loadDex.add(file);
            }
        }

        for (File dex : loadDex) {
            //加载获取本地的dexElement[],首先拿到PathClassLoader
            try {
                PathClassLoader classLoader = (PathClassLoader) context.getClassLoader();
                Class<?> baseClassLoader = classLoader.getClass().getSuperclass();
                Field pathListField = baseClassLoader.getDeclaredField("pathList");
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
                int myDexLength = Array.getLength(myDexElements);
                int originDexLength = Array.getLength(dexElementsValue);
                int newLength = myDexLength + originDexLength;
                //获取到数组的类型
                Class<?> componentType = dexElementsValue.getClass().getComponentType();
                Object newArrayElement = Array.newInstance(componentType, newLength);
                for (int i = 0; i < myDexLength; i++) {
                    if (i < myDexLength) {
                        Array.set(newArrayElement, i, Array.get(myDexElements, i));
                    } else {
                        Array.set(newArrayElement, i, Array.get(dexElementsValue, i - myDexLength));
                    }
                }
                //set新的数据
                dexElementsField.set(pathListField,newArrayElement);
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
