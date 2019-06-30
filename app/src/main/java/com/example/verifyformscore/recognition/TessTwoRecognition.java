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
        tessBaseAPI.setImage(recognitionBitmap);
        return isNumeric(tessBaseAPI.getUTF8Text());
    }

    /**
     * 去除字符串的空格，并判断是否全是数字
     *
     * @param scoreString 分数字符串
     * @return 处理后的字符串
     */
    private static Integer isNumeric(String scoreString) {
        scoreString = scoreString.replace(" ", "");
        scoreString = scoreString.trim();
        scoreString = scoreString.replaceAll("l", "1");
        scoreString = scoreString.replaceAll("o", "0");
        scoreString = scoreString.replaceAll("s", "5");
        scoreString = scoreString.replaceAll("b", "6");
        scoreString = scoreString.replaceAll("z", "2");
        if ("".equals(scoreString)) {
            scoreString = "0";
        }
        // 当有非数字或该单数图片识别出不知一位，则返回null
        if (!NUMBER_PATTERN.matcher(scoreString).matches()) {
            return -1;
        }
        return Integer.valueOf(scoreString);
    }
}
