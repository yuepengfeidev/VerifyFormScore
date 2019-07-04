package com.example.verifyformscore.recognition;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.regex.Pattern;

/**
 * TessTwoRecognition
 *
 * @author yuepengfei
 * @date 2019/6/29
 * @description 使用Tess-Two识别数字
 */
public class TessTwoRecognition {
    private final static Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    /**
     * 使用Tess-Two识别数字
     *
     * @param tessBaseAPI       tess-two
     * @param recognitionBitmap 数字图片
     * @return 返回识别数字结果
     */
    public static Integer recognizeNumberInTessTwo(TessBaseAPI tessBaseAPI, Bitmap recognitionBitmap) {
        // 设置识别图像结果为单个数字
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
        tessBaseAPI.setImage(recognitionBitmap);
        return isNumericResult(tessBaseAPI.getUTF8Text());
    }

    /**
     * 判断是否识别出正确数字结果
     *
     * @param scoreString 分数字符串
     * @return 处理后的字符串
     */
    private static Integer isNumericResult(String scoreString) {
        // 当有非数字或该单数图片识别不出结果，则返回null
        if (!NUMBER_PATTERN.matcher(scoreString).matches()
        || "".equals(scoreString)) {
            return -1;
        }
        return Integer.valueOf(scoreString);
    }
}
