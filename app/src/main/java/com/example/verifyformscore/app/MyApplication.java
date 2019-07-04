package com.example.verifyformscore.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;

import com.example.verifyformscore.recognition.TensorSlowRecognition;
import com.example.verifyformscore.utils.FileUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * MyApplication class
 *
 * @author yuepengfei
 * @date 2019/5/25
 */
public class MyApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    public static Context sContext;
    public static TessBaseAPI sTessBaseAPI;
    /**
     * 临时存放识别图片的文件
     */
    public static String sTempFileUriString;
    public static String sTempPicUriString;
    public static Map<String, Rect> sRectMap = new HashMap<>();
    public static TensorSlowRecognition sTensorSlowRecognition;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sTempFileUriString = sContext.getExternalFilesDir(null) + "/temp/";
    }

    public static void setInit(Context context) {
        // 初始化 TensorFlow
        sTensorSlowRecognition = new TensorSlowRecognition(context);
        // 初始化 tess-two
        sTessBaseAPI = new TessBaseAPI();
        // 添加白名单 和 黑名单
        String whiteList = "0123456789";
        String blackList = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+=-[]}{;:'\\\"\\\\|~`,./<>? ";
        sTessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);
        sTessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, blackList);
        // 字体库可通过 "+" 进行合并
        String lang = "number+number2+number3+number4+number5";
        // tesseract 指定设别的路径
        String dataPath = context.getFilesDir() + "/tesseract";
        // 字体库父文件的路径
        String filePath = dataPath + "/tessdata";

        if (FileUtils.checkFile(new File(filePath), dataPath, lang, context)) {
            sTessBaseAPI.init(dataPath, lang);
        }
    }

}
