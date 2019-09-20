package com.cymchad.tensor;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.Log;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 作者    cpf
 * 时间    2019/9/11 11:31
 * 文件    stylized-master
 * 描述
 */
public class RecyclerViewAdapter extends BaseQuickAdapter<RecyclerEntity, BaseViewHolder> {

    private int position;
    private Context context;

    public RecyclerViewAdapter(@LayoutRes int layoutResId, @Nullable List<RecyclerEntity> data, Context context) {
        super(layoutResId, data);
        this.context = context;
    }

    @Override
    protected void convert(final BaseViewHolder holder, RecyclerEntity item) {
        final AssetManager assetManager = context.getAssets();
        Bitmap bitmap = null;
        try {
            final InputStream inputStream = assetManager.open(item.getId());
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (final IOException e) {
            Log.d("stylized", "Error opening bitmap!", e);
        }

        holder.setImageBitmap(R.id.img, bitmap);
        /* 通过Activity传递position参数 做Item背景状态的切换*/
        holder.setBackgroundRes(R.id.rootview, holder.getLayoutPosition() == position ? R.color.colorAccent : R.color.colorPrimary);
    }

    public void setSelection(int pos) {
        this.position = pos;
        notifyDataSetChanged();
    }
}
