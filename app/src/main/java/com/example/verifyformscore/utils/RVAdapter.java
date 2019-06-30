package com.example.verifyformscore.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.verifyformscore.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RVAdapter
 *
 * @author yuepengfei
 * @date 2019/6/25
 * @description
 */
public class RVAdapter extends RecyclerView.Adapter {
    LayoutInflater mLayoutInflater;
    List<Bitmap> mBitmapList = new ArrayList<>();

    public RVAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setList(List<Bitmap> bitmapList) {
        this.mBitmapList = bitmapList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = mLayoutInflater.inflate(R.layout.item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        Bitmap bitmap = mBitmapList.get(i);
        holder.ivItem.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return mBitmapList.size();
    }

    static
    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_item)
        ImageView ivItem;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
