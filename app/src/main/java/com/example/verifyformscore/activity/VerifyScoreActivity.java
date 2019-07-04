package com.example.verifyformscore.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.verifyformscore.R;
import com.example.verifyformscore.app.MyApplication;
import com.example.verifyformscore.recognition.TessTwoRecognition;
import com.example.verifyformscore.utils.FileUtils;
import com.example.verifyformscore.utils.FormDisposeUtils;
import com.example.verifyformscore.utils.RVAdapter;
import com.example.verifyformscore.widget.MyToast;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class VerifyScoreActivity extends AppCompatActivity {

    @BindView(R.id.iv_form)
    ImageView ivForm;
    @BindView(R.id.tv_score)
    TextView tvScore;
    @BindView(R.id.iv_result)
    ImageView ivResult;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.rv_score)
    RecyclerView rvScore;
    private String scoreString = "score",
            stuInfoString = "stuInfo", testDetailString = "testDetail";
    TessBaseAPI mTessBaseAPI;
    /**
     * 容纳 RaJava dispose 的容器，用于一次性销毁处理
     */
    CompositeDisposable compositeDisposable;
    Bitmap scoreBitMap;

    RVAdapter mRVAdapter;
    MyToast mToast;
    String recognizeType;
    final static String TESSTWO_TYPE = "TessTwo";
    final static String TENSORFLOW_TYPE = "TensorFlow";
    /**
     * 最少5各模块（4个分值模块 + 1个总分模块）
     */
    final static int MIN_SOCRE_ITEM_COUNT = 5;
    /**
     * 各模块分数的列表
     */
    List<Integer> scoreResults = new ArrayList<>();
    List<Bitmap> mBitmapList = new ArrayList<>();

    boolean isFisrtLoad = true;

    /**
     * 创建按序执行任务线程池
     */
    ExecutorService mExecutorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("RecognitionThread");
            return thread;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_score);
        ButterKnife.bind(this);

        recognizeType = getIntent().getStringExtra("type");

        mTessBaseAPI = MyApplication.sTessBaseAPI;
        scoreBitMap = FileUtils.getImage(scoreString);
        compositeDisposable = new CompositeDisposable();
        initWidget();
    }

    private void initWidget() {
        ivForm.setImageBitmap(scoreBitMap);
        progressBar.setVisibility(View.VISIBLE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRVAdapter = new RVAdapter(this);
        rvScore.setLayoutManager(layoutManager);
        rvScore.setAdapter(mRVAdapter);

        mToast = new MyToast(this);
    }

    /**
     * 处理提取表格分数模块
     *
     * @param bitmap 表格模块图片
     */
    private void disposeForm(final Bitmap bitmap) {
        isFisrtLoad = false;
        Observable<Map<Integer, List<Bitmap>>> observable = Observable.create(new ObservableOnSubscribe<Map<Integer, List<Bitmap>>>() {
            @Override
            public void subscribe(ObservableEmitter<Map<Integer, List<Bitmap>>> emitter) {
                emitter.onNext(FormDisposeUtils.disposeFormPic(bitmap));
            }
        });
        Disposable disposable = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Map<Integer, List<Bitmap>>>() {
                    @Override
                    public void accept(Map<Integer, List<Bitmap>> scoreMap) {
                        if (scoreMap.size() < MIN_SOCRE_ITEM_COUNT) {
                            progressBar.setVisibility(View.GONE);
                            mToast.showToast("你拍照技术不行呀！重拍一下呗。");
                        } else {
                            // 异步一张一张图片按顺序识别处理显示
                            recognizeScore(scoreMap);
                        }
                    }
                });
        // 将RxJava的dispose添加进CompositeDisposable，一次性处理
        compositeDisposable.add(disposable);
    }

    /**
     * tess-two识别数字
     *
     * @param scoreMap 各个分数模块的数字图片的列表
     */
    private void recognizeScore(final Map<Integer, List<Bitmap>> scoreMap) {
        boolean isLast = false;
        List<Map.Entry<Integer, List<Bitmap>>> mapList = new ArrayList<>(scoreMap.entrySet());
        for (Map.Entry<Integer, List<Bitmap>> map : mapList) {
            List<Bitmap> bitmaps = map.getValue();
            // 如果有分数板块为获取到数字图片，则重拍
            if (bitmaps.size() == 0) {
                progressBar.setVisibility(View.GONE);
                mToast.showToast("你拍照技术不行呀！重拍一下呗。");
                mExecutorService.shutdownNow();
                return;
            }
            for (final Bitmap bitmap : bitmaps) {
                mBitmapList.add(bitmap);
                // 当识别到最后一个模块的分数,且最后一位数字时，提醒是最后一张数字识别图片
                if (mapList.indexOf(map) == (scoreMap.size() - 1) &&
                        bitmaps.indexOf(bitmap) == (bitmaps.size() - 1)) {
                    isLast = true;
                }
                MyThread thread = new MyThread(map.getKey(), bitmap, isLast);
                mExecutorService.execute(thread);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    // 只有初始化完OpenCV后才能使用
                    if (isFisrtLoad) {
                        disposeForm(scoreBitMap);
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public class MyThread implements Runnable {
        /**
         * 对应分数位置
         */
        final int mIndex;
        /**
         * 待识别的数字图片
         */
        final Bitmap mBitmap;
        /**
         * 是否是最后一位，用于判断结束识别，之后进行ui操作
         */
        final boolean mIsLast;

        MyThread(int index, Bitmap bitmap, boolean isLast) {
            mIndex = index;
            mBitmap = bitmap;
            mIsLast = isLast;
        }

        @Override
        public void run() {
            int number = -1;
            if (recognizeType.equals(TESSTWO_TYPE)) {
                number = TessTwoRecognition.recognizeNumberInTessTwo(MyApplication.sTessBaseAPI, mBitmap);
            } else if (recognizeType.equals(TENSORFLOW_TYPE)) {
                number = MyApplication.sTensorSlowRecognition.recognizeNumberInTensorSlow(mBitmap);
            }
            // 如果有无法识别的数字，则立刻关闭线程，不再执行识别
            if (number == -1) {
                mExecutorService.shutdownNow();
                mHandler.sendEmptyMessage(0);
            }
            // 当前分数列表没有分数 或 当前识别的分数对应的位置与分数列表的最后索引不一致，
            // 表示分数列表中还没有该位置的分数
            if (scoreResults.size() == 0 || (scoreResults.size() - 1) != mIndex) {
                scoreResults.add(mIndex, number);
            }// 否则完整补充该位置的分数
            else {
                // 该位置先前数字作为高位，当前数字作为低位，相加组成新的分数
                int score = Integer.valueOf(String.valueOf(scoreResults.get(mIndex)) + number);
                scoreResults.set(mIndex, score);
            }
            // 识别完所有数字，进行验证分数/UI处理
            if (mIsLast) {
                verifyScore(scoreResults);
            }
        }
    }

    /**
     * 验证 所有分数模块 和 总分是否匹配，并显示结果
     *
     * @param scoreList 所有分数模块列表
     */
    private void verifyScore(List<Integer> scoreList) {
        // 各模块分值加起来的准确总分
        int totalScore = 0;
        // 识别的总分
        int rcTotalScore = 0;
        // 组合的各模块分数相加的式子
        StringBuilder formulaString = new StringBuilder();
        for (int i = 0; i < scoreList.size(); i++) {
            int score = scoreList.get(i);
            // 最后一位是总分
            if (i < scoreList.size() - 1) {
                totalScore += score;
                if (i < (scoreList.size() - 2)) {
                    String s = score + " + ";
                    formulaString.append(s);
                } else {
                    formulaString.append(score);
                }
            } else {
                rcTotalScore = score;
                String s = " = " + totalScore;
                formulaString.append(s);
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString("formulaString", formulaString.toString());
        bundle.putInt("rcTotalScore", rcTotalScore);
        bundle.putInt("totalScore", totalScore);
        Message message = new Message();
        message.what = 1;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdownNow();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 一次性处理 容器中的 所有dispose
        if (compositeDisposable != null && compositeDisposable.size() > 0) {
            compositeDisposable.dispose();
            compositeDisposable.clear();
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    progressBar.setVisibility(View.GONE);
                    mToast.showToast("这字我不太认识。。");
                    break;
                case 1:
                    Bundle bundle = msg.getData();
                    String mFormulaString = bundle.getString("formulaString");
                    int mRcTotalScore = bundle.getInt("rcTotalScore");
                    int mTotalScore = bundle.getInt("totalScore");
                    mRVAdapter.setList(mBitmapList);
                    progressBar.setVisibility(View.GONE);
                    if (mRcTotalScore == mTotalScore) {
                        ivResult.setImageResource(R.drawable.correct_icon);
                        tvScore.setText(mFormulaString);
                    } else {
                        ivResult.setImageResource(R.drawable.query_icon);
                        tvScore.setText(mFormulaString);
                    }
                    break;
                default:
            }
            return false;
        }
    });


}
