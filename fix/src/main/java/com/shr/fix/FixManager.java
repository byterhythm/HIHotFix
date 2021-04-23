package com.shr.fix;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FixManager {

    private File downLoad(String dexUrl) {
        String dexName = "out.dex";
        return new File(Environment.getExternalStorageDirectory() + File.separator + dexName);
    }

    public void loadDex(Context context) {
        //1.这里展示从网络已经下载到本地文件，读取该文件
        File downLoadFile = downLoad("xxx");
        //2.copy 该文件到应用程序私有目录/data/data/应用包名/xx
        File file = context.getDir("odex", Context.MODE_PRIVATE);

    }
}
