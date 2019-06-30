package com.example.verifyformscore.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.example.verifyformscore.app.MyApplication;
import com.example.verifyformscore.utils.MeasureUtils;

/**
 * RectangleView
 *
 * @author yuepengfei
 * @date 2019/6/23
 * @description 铺在相机界面上的 定位线
 */
public class RectangleView extends View {
    private Paint mRectPaint;
    private Paint mLinePaint;
    /**
     * 试卷详情选区
     */
    private Rect testDetailRect;

    /**
     * 屏幕宽高
     */
    private int mDisplayWidth;
    private int mDisplayHeight;

    /**
     * 十字定位线交点的 X Y坐标
     */
    int pointX;
    int pointY;

    public RectangleView(Context context) {
        super(context);
        init();
    }

    public RectangleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRectPaint.setColor(Color.WHITE);
        mRectPaint.setStyle(Paint.Style.STROKE);
        // 设置矩形角落为平角
        mRectPaint.setStrokeJoin(Paint.Join.BEVEL);
        mRectPaint.setStrokeWidth(1.5f);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(Color.BLUE);

        // 获取屏幕宽高
        MeasureUtils measureUtils = MeasureUtils.getInstance(getContext());
        mDisplayHeight = measureUtils.getDisplayHeight();
        mDisplayWidth = measureUtils.getDisplayWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLocationLine(canvas);
        drawStuInfoRect(canvas);
        drawTestDetailRect(canvas);
        drawScoreRect(canvas);
    }

    /**
     * 绘制十字定位线
     */
    private void drawLocationLine(Canvas canvas) {
        // 十字交点 x y 坐标
        pointX = mDisplayWidth / 100 * 20;
        pointY = mDisplayHeight / 100 * 28;
        canvas.drawLine(pointX, 0, pointX, mDisplayHeight, mLinePaint);
        canvas.drawLine(0, pointY, mDisplayWidth, pointY, mLinePaint);
    }

    /**
     * 绘制考卷详情选区
     */
    private void drawTestDetailRect(Canvas canvas) {
        int testDetailHeight = mDisplayHeight / 100 * 14;
        int testDetailWidth = mDisplayWidth / 100 * 78;
        testDetailRect = new Rect(pointX + pointX / 100 * 14, pointY - testDetailHeight,
                pointX + pointX / 100 * 17 + testDetailWidth, pointY - 3);
        canvas.drawRect(testDetailRect, mRectPaint);
        String testDetailString = "testDetail";
        if (MyApplication.sRectMap.get(testDetailString) == null) {
            MyApplication.sRectMap.put(testDetailString, testDetailRect);
        }
    }

    /**
     * 绘制分数模块区域
     */
    private void drawScoreRect(Canvas canvas) {
        int scoreHeight = mDisplayHeight / 15;
        int scoreWidth = mDisplayWidth / 100 * 70;
        /*
         * 分数板块选区
         */
        Rect scoreRect = new Rect(testDetailRect.left, pointY + pointX / 10,
                testDetailRect.left + scoreWidth, pointY + pointX / 10 + scoreHeight);
        canvas.drawRect(scoreRect, mRectPaint);
        String scoreString = "score";
        if (MyApplication.sRectMap.get(scoreString) == null) {
            MyApplication.sRectMap.put(scoreString, scoreRect);
        }
    }

    /**
     * 绘制学生信息选区
     */
    private void drawStuInfoRect(Canvas canvas) {
        int stuInfoHeight = mDisplayHeight / 100 * 45;
        /*
         * 学生信息选区
         */
        Rect stuInfoRect = new Rect(0, pointY + 3,
                pointX / 100 * 67, pointY + stuInfoHeight);
        canvas.drawRect(stuInfoRect, mRectPaint);
        String stuInfoString = "stuInfo";
        if (MyApplication.sRectMap.get(stuInfoString) == null) {
            MyApplication.sRectMap.put(stuInfoString, stuInfoRect);
        }
    }
}
