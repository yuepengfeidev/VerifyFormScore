package com.example.verifyformscore.recognition;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * TensorSlowRecognition
 *
 * @author yuepengfei
 * @date 2019/6/29
 * @description TensorSlows识别，与训练数字库对别识别得出最优结果
 */
public class TensorSlowRecognition {
    /**
     * 训练数字模型文件位置
     */
    private static final String MODEL_PATH = "file:///android_asset/model/NumTrans_Graph.pb";

    /**
     * 设置模型输入/输出节点的数据维度
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_IMG_SIZE_HEIGHT = 28;
    private static final int DIM_IMG_SIZE_WIDTH = 28;
    private static final int DIM_PIXEL_SIZE = 1;

    private static final int CATEGORY_COUNT = 10;

    /**
     * 模型中输入变量的名称
     */
    private static final String INPUT_NAME = "input";
    /**
     * 模型中输出变量的名称
     */
    private static final String OUTPUT_NAME = "output";
    private static final String[] OUTPUT_NAMES = {OUTPUT_NAME};

    private final int[] mImagePixels = new int[DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH];
    private final float[] mImageData = new float[DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH];
    private final float[] mResult = new float[CATEGORY_COUNT];

    private TensorFlowInferenceInterface mInferenceInterface;

    public TensorSlowRecognition(Context context) {
        mInferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_PATH);
    }

    /**
     * 识别数字
     *
     * @param bitmap 识别的数字图片
     * @return 识别数字结果
     */
    public int recognizeNumberInTensorSlow(Bitmap bitmap) {
        convertBitmap(bitmap);

        // 将数据feed给TensorFlow的输入节点
        mInferenceInterface.feed(INPUT_NAME, mImageData, DIM_BATCH_SIZE, DIM_IMG_SIZE_HEIGHT,
                DIM_IMG_SIZE_WIDTH, DIM_PIXEL_SIZE);
        // 进行模型推理
        mInferenceInterface.run(OUTPUT_NAMES);
        // 获取输出节点的输出信息, 存储模型的输出数据到数组
        mInferenceInterface.fetch(OUTPUT_NAME, mResult);
        return getResult(mResult);
    }

    private void convertBitmap(Bitmap bitmap) {
        bitmap.getPixels(mImagePixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        /*for (int i = 0; i < DIM_IMG_SIZE_WIDTH * DIM_IMG_SIZE_HEIGHT; i++) {
            // 二值图(黑字白底)，黑色部分值为-16777216 白色部分为-1，要将黑色改为2ff，白色改为0
            // 改为黑底白字，才能对比识别
            if (mImagePixels[i] == -1) {
                mImageData[i] = 0;
            } else {
                mImageData[i] = 255f;
            }
        }*/

        // 黑字白底 转为 黑底白字
        for (int i = 0; i < mImagePixels.length; ++i) {
            int pix = mImagePixels[i];
            pix = pix & 0xff;
            int b = pix & 0xff;
            mImageData[i] = 0xff - b;
        }
    }

    /**
     * 通过比较所有可能的数字的概率，挑去概率最大的作为最终结果
     *
     * @param probs 所有可能的数字概率
     * @return 最终结果
     */
    private int getResult(float[] probs) {
        int maxIdx = -1;
        float maxProb = 0.0f;
        for (int i = 0; i < probs.length; i++) {
            if (probs[i] > maxProb) {
                maxProb = probs[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}
