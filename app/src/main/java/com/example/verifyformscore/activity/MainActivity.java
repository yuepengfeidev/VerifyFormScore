package com.example.verifyformscore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.example.verifyformscore.R;
import com.example.verifyformscore.app.MyApplication;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.verifyformscore.utils.PermissionsUtils.PERMISSION_REQUEST_CODE;
import static com.example.verifyformscore.utils.PermissionsUtils.hasPermissions;
import static com.example.verifyformscore.utils.PermissionsUtils.permissions;
import static com.example.verifyformscore.utils.PermissionsUtils.requestNecessaryPermissions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        onViewClicked(getWindow().getDecorView());
        // 没有权限则获取权限
        if (!hasPermissions(permissions)) {
            requestNecessaryPermissions(this, permissions);
        } else {
            mHandler.sendEmptyMessage(0);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && !hasPermissions(permissions)) {
            Toast.makeText(getApplicationContext(), "请确定打开所有权限", Toast.LENGTH_LONG).show();
            finish();
        } else {
            mHandler.sendEmptyMessage(0);
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            MyApplication.setInit(MainActivity.this);
            return false;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    @OnClick({R.id.bt_tess_two, R.id.bt_tensorflow})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt_tess_two:
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra("type","TessTwo");
                startActivity(intent);
                break;
            case R.id.bt_tensorflow:
                Intent intent2 = new Intent(MainActivity.this, CameraActivity.class);
                intent2.putExtra("type", "TensorFlow");
                startActivity(intent2);
                break;
                default:
        }
    }
}
