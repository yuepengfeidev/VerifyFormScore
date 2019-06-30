package com.example.verifyformscore.widget;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.verifyformscore.R;

/**
 * Created by 你是我的 on 2019/4/1
 */
public class MyToast{
    private Toast mToast;
    private Activity mActivity;

    public MyToast(Activity activity) {
        mActivity = activity;
    }

    public void showToast(String content){
        if (mToast == null){
            mToast = new Toast(mActivity);
            View view = View.inflate(mActivity, R.layout.layout_toast,null);
            TextView tvToast = view.findViewById(R.id.tv_toast);
            tvToast.setText(content);
            mToast.setView(view);
            mToast.setGravity(Gravity.CENTER, 0, -100);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }else {
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }
}
