package com.example.verifyformscore.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.verifyformscore.R;
import com.example.verifyformscore.widget.MyCameraView;
import com.example.verifyformscore.widget.RectangleView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity {

    @BindView(R.id.camera_view)
    MyCameraView cameraView;
    @BindView(R.id.rectangle_view)
    RectangleView rectangleView;
    @BindView(R.id.bt_take_photo)
    Button mButton;

    String type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideBottomUIMenu();
            setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        onViewClicked(getWindow().getDecorView());

        type = getIntent().getStringExtra("type");
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume(this);
        hideBottomUIMenu();
        mButton.setClickable(true);
    }

    @Override
    protected void onPause() {
        cameraView.onPause();
        super.onPause();
    }


    @OnClick({R.id.bt_take_photo, R.id.iv_exit})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_take_photo:
                // 设置相机拍照后将要识别的方式
                cameraView.setType(type);
                cameraView.takePicture();
                mButton.setClickable(false);
                break;
            case R.id.iv_exit:
                finish();
                overridePendingTransition(0, R.anim.out);
                break;
            default:
        }
    }

    /**
     * 隐藏虚拟按键,后两个uiOptions是为了设置状态栏透明
     */
    protected void hideBottomUIMenu() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }
}
