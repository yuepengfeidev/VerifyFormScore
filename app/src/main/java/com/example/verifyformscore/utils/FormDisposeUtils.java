package com.example.verifyformscore.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * FormDisposeUtils
 *
 * @author yuepengfei
 * @date 2019/6/20
 * @description 表格图片处理工具
 */
public class FormDisposeUtils {
    /**
     * 最少两行7列，也就是3条横向分割线，8条竖向分割线
     */
    private static final int MIN_COL_COUNT = 8;
    private static final int MIN_ROW_COUNT = 3;

    /**
     * 对表格图片进行处理
     *
     * @param bitmap 表格图片
     * @return 表格图片分割出来得各分数模块图片
     */
    public static Map<Integer,List<Bitmap>> disposeFormPic(Bitmap bitmap) {
        Mat mat = new Mat();
        Mat disposeMat = new Mat();

        Utils.bitmapToMat(bitmap, mat);
        // 灰度化
        Imgproc.cvtColor(mat, disposeMat, Imgproc.COLOR_BGR2GRAY);
        // 自动阈值二值化，blockSize属性为加粗二值化后的边框线条
        Imgproc.adaptiveThreshold(disposeMat, disposeMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY_INV, 7, 10);

        // 获取表格横纵线
        Mat horizontal = disposeMat.clone();
        Mat vertical = disposeMat.clone();

        // 经过二次膨胀后，更加详细表格分隔线，便于检出表格几行几列
        Mat detailVertical = new Mat();
        Mat detailHorizontal = new Mat();

        // scale越大，获取的直线时越精细，得到的直线也越多
        int vScale = 30;
        int horizontalSize = horizontal.cols() / vScale;
        // 为了获取横向的表格线，设置腐蚀和膨胀的操作区域为一个比较大的横向直条
        Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalSize, 1));
        Point point = new Point(-1, -1);
        // 先腐蚀再膨胀
        Imgproc.erode(horizontal, horizontal, horizontalStructure);
        Imgproc.dilate(horizontal, horizontal, horizontalStructure);
        // 再次膨胀，突出表格分隔线(针对表格外边框有破损短线，无法识别为直线，再次膨胀可解决，但膨胀耗时大，无特殊情况可不用)
        Imgproc.dilate(horizontal, detailHorizontal, horizontalStructure, point, 1);

        // 竖线较多，设置小一些
        int hScale = 4;
        int verticalSize = vertical.rows() / hScale;
        Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalSize));
        Imgproc.erode(vertical, vertical, verticalStructure);
        Imgproc.dilate(vertical, vertical, verticalStructure);
        Imgproc.dilate(vertical, detailVertical, verticalStructure, point, 1);

        // 通过腐蚀和膨胀操作去除噪声
        //腐蚀膨胀计算的核心
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
        // 腐蚀化处理
        Imgproc.erode(disposeMat, disposeMat, kernel);
        // 膨胀化处理
        Imgproc.dilate(disposeMat, disposeMat, kernel);

        Mat mask = new Mat();
        Core.add(vertical, horizontal, mask);

        List<MatOfPoint> counters = new ArrayList<>();
        List<MatOfPoint> verticalCounters = new ArrayList<>();
        List<MatOfPoint> horizontalCounters = new ArrayList<>();
        // 寻找表格外部轮廓
        Imgproc.findContours(mask, counters, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        int index = 0;
        // 面积最大的则是表格的最外边轮廓
        for (int i = 0; i < counters.size(); i++) {
            double area = Imgproc.contourArea(counters.get(i));
            if (area > maxArea) {
                maxArea = area;
                index = i;
            }
        }
        if (counters.size() == 0) {
            return new TreeMap<>();
        }

        // 表格Mat
        Mat formMat = new Mat();
        MatOfPoint matOfPoint;
        MatOfPoint2f matOfPoint2f;
        matOfPoint = counters.get(index);
        // MatOfPoint 转 MatOfPoint2f
        matOfPoint2f = new MatOfPoint2f(matOfPoint.toArray());

        // 获取该表格的旋转矩阵（最小最贴近边缘的矩阵）
        RotatedRect rotatedRect = Imgproc.minAreaRect(matOfPoint2f);

        /*
         * 以举证左下角的点做平行于x轴的直线，将直线逆时针旋转到第一条矩阵的边缘的角度
         * 即为minAreaRect的angle，且该边缘视为矩阵的width（很重要！！）
         * 该角度在0到-90之间，矩阵向左倾斜时，角度从0到90，
         * 举证向右倾斜时，角度从-90到0。
         * warpAffine逆时针旋转时为正角度，当矩阵左倾斜时矩阵的宽就为举证底部边缘的宽，
         * 它需要以矩阵中心点顺时针旋转angle（顺时针为负角度）
         * 当矩阵右倾斜时，矩阵右边的边缘视为矩阵的width，
         * 它需要以矩阵中心点你是逆时针旋转角度，此时angle是从-90到0的，
         * 所以旋转的角度应为 angle + 90（正数）
         * 由于右倾斜时，矩阵的width为其高，矩阵的height为其宽，
         * 所以需要反转一下，重新设置Size，否则getRectSubPix截取矩阵图片时会出问题
         */
        double rotationAngle;
        Size size;
        // 因为膨胀加粗原因，下面通过纵横线截取分数块图片时需要添加偏移量
        int offest;
        if (rotatedRect.angle > -45) {
            rotationAngle = rotatedRect.angle;
            size = rotatedRect.size;
            offest = 8;
        } else {
            rotationAngle = 90 + rotatedRect.angle;
            size = new Size(new Point(rotatedRect.size.height, rotatedRect.size.width));
            offest = 11;
        }

        // 把旋转矩阵 转为 mat
        Mat rotMat = Imgproc.getRotationMatrix2D(rotatedRect.center, rotationAngle, 1);
        // 旋转 矫正 原图
        Imgproc.warpAffine(disposeMat, disposeMat, rotMat, disposeMat.size());
        // 截取旋转矫正后的表格，原图必须要设置为 COLOR_RGBA2RGB（不能为4通道，必须3通道）
        Imgproc.getRectSubPix(disposeMat, size, rotatedRect.center, formMat);

        // 将纵横表格分隔线的二值化图也旋转截取表格，方便获取表格纵横线
        Imgproc.warpAffine(detailVertical, detailVertical, rotMat, detailVertical.size());
        Imgproc.getRectSubPix(detailVertical, size, rotatedRect.center, detailVertical);
        Imgproc.warpAffine(detailHorizontal, detailHorizontal, rotMat, detailHorizontal.size());
        Imgproc.getRectSubPix(detailHorizontal, size, rotatedRect.center, detailHorizontal);

        // 找出表格的纵表格所有纵横向线 的轮廓
        Imgproc.findContours(detailVertical, verticalCounters, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(detailHorizontal, horizontalCounters, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // 表格的宽度
        int formWidth = formMat.width();

        // 存放表格横向分隔线的Rect
        List<Rect> hLinesList = new ArrayList<>();
        // 表格行数
        int rowCount = 0;
        // 最后一行表格的矩阵坐标
        int topY, leftX = 0, bottomY, formTop;
        for (MatOfPoint mop : horizontalCounters) {
            Rect r = Imgproc.boundingRect(mop);
            // 如果小于 表格宽度的五分之四，则表示该条直线不是表格的横向分隔线
            if (r.width < (formWidth / 5) * 4) {
                continue;
            }

            // 绘制出表格横向分隔线
            /*Imgproc.drawContours(formMat, horizontalCounters, horizontalCounters.indexOf(mop),
                    new Scalar(255, 255, 0), 3, 4, new Mat(), 0, new Point(0, 0));*/
            rowCount++;

            hLinesList.add(r);
        }
        // 如果横向分割线少于 3 条（两行），重拍
        if (rowCount < MIN_ROW_COUNT) {
            return new TreeMap<>();
        }

        // 按y坐标升序
        Collections.sort(hLinesList, new YAscComparator());
        // 最后一条横向分隔线的y坐标为 表格的bottom
        bottomY = hLinesList.get(hLinesList.size() - 1).y;
        // 倒数第二条横向分隔线的y坐标为 表格最后一行的top
        topY = hLinesList.get(hLinesList.size() - 2).y + offest;
        // 第一条的y坐标为表格的top
        formTop = hLinesList.get(0).y;

        int formHeight = bottomY - formTop;

        // 截取表格最后一行
        formMat = formMat.submat(new Rect(leftX, topY, formWidth, bottomY - topY));

        // 存放表格纵线的Rect
        List<Rect> vLinesList = new ArrayList<>();
        // 表格列数
        int colCount = 0;
        for (MatOfPoint mop : verticalCounters) {
            Rect r = Imgproc.boundingRect(mop);
            if (r.height < formHeight / 10 * 9) {
                continue;
            }
            colCount++;
            vLinesList.add(r);
        }
        // 如果纵向分割线少于8条（7列），重拍
        if (colCount < MIN_COL_COUNT) {
            return new TreeMap<>();
        }
        // 按x坐标升序
        Collections.sort(vLinesList, new XAscComparator());


        // 存放各块分数的mat
        List<Mat> mats = new ArrayList<>();
        for (int i = 1; i < vLinesList.size() - 2; i++) {
            Rect lineRect = vLinesList.get(i);
            Rect lineRect2 = vLinesList.get(i + 1);
            // 截取每块分数的图片
            Mat m = formMat.submat(new Rect(lineRect.x + offest, 0,
                    lineRect2.x - lineRect.x - offest - 3, formMat.height()));
            mats.add(m);
        }

        // 各分数模块按从左到右的位置排序
        Map<Integer, List<Bitmap>> scoreMap = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 > o2) {
                    return 1;
                } else if (o1 < o2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        for (Mat m : mats) {
            List<MatOfPoint> matOfPoints = new ArrayList<>();

            // 寻找每个分数板块中 每个数字的轮廓
            Imgproc.findContours(m, matOfPoints, new Mat(), Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);
            List<Rect> rectList = new ArrayList<>();
            for (MatOfPoint mop : matOfPoints) {
                // 将每个数字用矩形框起来
                Rect rect = Imgproc.boundingRect(mop);
                // 添加条件，过滤杂线
                if (rect.height < (m.height() / 5)) {
                    continue;
                }
                rectList.add(rect);
            }
            // 按x坐标排序
            Collections.sort(rectList, new XAscComparator());
            List<Bitmap> bitmaps = new ArrayList<>();
            for (Rect rect : rectList) {

                // 将每个数字分割出来
                Mat scoreMat = m.submat(rect);
                // 扩充图片边缘9，将图片放大
                Core.copyMakeBorder(scoreMat, scoreMat, 9, 9, 9, 9, Core.BORDER_ISOLATED);

                Size sz = new Size(28, 28);
                // 调整个数字图片为28*28,方便TensorFlow对比数字库识别
                Imgproc.resize(scoreMat, scoreMat, sz);
                // 将黑底白字二值化为黑字白底,便于识别
                Imgproc.adaptiveThreshold(scoreMat, scoreMat, 255,
                        Imgproc.ADAPTIVE_THRESH_MEAN_C,
                        Imgproc.THRESH_BINARY_INV, 23, -1);
                Bitmap b = Bitmap.createBitmap(scoreMat.width(), scoreMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(scoreMat, b);
                bitmaps.add(b);
            }
            scoreMap.put(mats.indexOf(m), bitmaps);
        }

        return scoreMap;
    }

    /**
     * 表格纵分隔线按x坐标升序排序
     */
    public static class XAscComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            Rect rect1 = (Rect) o1;
            Rect rect2 = (Rect) o2;
            int x1 = rect1.x;
            int x2 = rect2.x;
            return Integer.compare(x1, x2);
        }
    }


    /**
     * 表格横分隔线按y坐标升序排序
     */
    public static class YAscComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            Rect rect1 = (Rect) o1;
            Rect rect2 = (Rect) o2;
            int y1 = rect1.y;
            int y2 = rect2.y;

            return Integer.compare(y1, y2);
        }
    }
}
