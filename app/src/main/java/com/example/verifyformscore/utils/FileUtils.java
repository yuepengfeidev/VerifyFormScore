package com.example.verifyformscore.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.example.verifyformscore.app.MyApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FileUtils class
 *
 * @author yuepengfei
 * @date 2019/5/25
 * @description 文件处理工具
 */
public class FileUtils {
    private static String TAG = "FileUtils";

    /**
     * 检查文件
     *
     * @param dir      存放资源的文件
     * @param dataPath 字体库文件路径
     * @param lang     字体库 语种
     * @param context  context
     * @return 是否完成检查
     */
    public static boolean checkFile(File dir, String dataPath, String lang, Context context) {
        // 如果没有该文件则创建，然后复制
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(dataPath, lang, context);
        }
        if (dir.exists()) {
            String dataFilePath = dataPath + "/tessdata/" + lang + ".traineddata";
            File dataFile = new File(dataFilePath);
            if (!dataFile.exists()) {
                copyFiles(dataPath, lang, context);
            }
        }
        return true;
    }

    /**
     * 将assets中的字体库 复制到 tess-two 指定读取的文件
     *
     * @param dataPath 字体库文件路径
     * @param lang     字体库 语种
     * @param context  context
     */
    private static void copyFiles(String dataPath, String lang, Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream inputStream;
        OutputStream outputStream;
        String[] langArray = lang.split("\\+");

        for (String l : langArray) {
            try {
                String fileName = "tessdata/" + l + ".traineddata";
                // 打开 assets 中的资源
                inputStream = assetManager.open(fileName);
                String destFile = dataPath + "/" + fileName;
                outputStream = new FileOutputStream(destFile);

                // 读取复制
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "copyFiles: " + "复制Tess-Two数字训练库失败");
            }
        }
    }

    /**
     * 存储识别原图
     *
     * @param bytes 源图的字节数组
     */
    public static void saveImage(byte[] bytes) {
        File file = new File(MyApplication.sTempFileUriString);
        if (!file.exists()) {
            boolean result = file.mkdirs();
            if (result) {
                Log.d(TAG, "saveImage: " + "创建文件夹成功");
            } else {
                Log.d(TAG, "saveImage: " + "创建文件夹失败");
            }
        }
        File f = new File(file.getAbsolutePath(), "tempPic.jpg");
        if (!f.exists()) {
            boolean result;
            try {
                result = f.createNewFile();
                if (result) {
                    Log.d(TAG, "saveImage: " + "创建临时图片成功");
                } else {
                    Log.d(TAG, "saveImage: " + "创建临时图片失败");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        MyApplication.sTempPicUriString = f.getAbsolutePath();
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(f);
            outputStream.write(bytes);
            Log.d(TAG, "saveImage: " + "存储临时图片成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "saveImage: " + "存储临时图片失败");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取识别原图，并按选区截取获得各模块的图片
     *
     * @param category 选区类别
     * @return 指定类别图片
     */
    public static Bitmap getImage(String category) {
        Rect rect = MyApplication.sRectMap.get(category);
        assert rect != null;
        Bitmap bitmap = BitmapFactory.decodeFile(MyApplication.sTempPicUriString);
        bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top,
                rect.width(), rect.height());
        return bitmap;
    }
}
