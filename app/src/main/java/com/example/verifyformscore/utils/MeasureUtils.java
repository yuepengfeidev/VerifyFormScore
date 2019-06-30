package com.example.verifyformscore.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * MeasureUtils
 *
 * @author yuepengfei
 * @date 2019/6/23
 * @description 测量工具
 */
public class MeasureUtils {
    private static MeasureUtils sMeasureUtils;
    private int mDisplayWidth;
    private int mDisplayHeight;

    public static MeasureUtils getInstance(Context context) {
        if (sMeasureUtils == null) {
            sMeasureUtils = new MeasureUtils(context);
        }
        return sMeasureUtils;
    }

    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    public MeasureUtils(Context context) {
        // 获取屏幕高度
        if (mDisplayHeight == 0 || mDisplayWidth == 0) {
            // 宽高还未负值
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(displayMetrics);
                if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                    // 横屏
                    mDisplayWidth = displayMetrics.heightPixels;
                    mDisplayHeight = displayMetrics.widthPixels;
                }else {
                    // 竖屏
                    mDisplayWidth = displayMetrics.widthPixels;
                    mDisplayHeight = displayMetrics.heightPixels;
                }
            }
        }
    }

    /**
     * 获取状态栏高度
     */
    public int  getStatusBarHeight(Context context)
    {
        int resID = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        // 可以获取到的状态栏的高度
        if (resID > 0) {
            return context.getResources().getDimensionPixelSize(resID);
        }
        return 0;
    }

}
