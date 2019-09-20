package com.cymchad.tensor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;

public class TensorActivity extends AppCompatActivity {
    private static final String MODEL_FILE = "stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final int NUM_STYLES = 26;
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;

    private TensorFlowInferenceInterface inferenceInterface;
    private Handler handler;

    private long lastProcessingTimeMs;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap textureCopyBitmap;

    private ImageView imageView, origin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tensor);
        imageView = findViewById(R.id.image);
        origin = findViewById(R.id.origin_image);
        croppedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.beauty_1);
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, desiredSize, desiredSize, false);
        imageView.setImageBitmap(croppedBitmap);
        origin.setImageBitmap(croppedBitmap);
        origin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TensorActivity.this,TestTensorActiv.class);
                startActivity(intent);
            }
        });
        //创建列表布局管理
        recyclerView = findViewById(R.id.recyclerView);
        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        //创建适配器
        adapter = new RecyclerViewAdapter(R.layout.item_recyclerview, initData(), this);
        //设置适配器
        recyclerView.setAdapter(adapter);
        //打开默认动画
        adapter.openLoadAnimation();
        initListener(this);
    }


    // 为适配器填充数据
    private ArrayList<RecyclerEntity> initData() {
        ArrayList<RecyclerEntity> list = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            RecyclerEntity entify = new RecyclerEntity();
            entify.setId("models/style" + i + ".jpg");
            list.add(entify);
        }
        return list;
    }

    // 初始化列表相关的点击事件
    private void initListener(final Context context) {
        /* 该接口针对整个Item布局 可传递position参数*/
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter baseAdapter, View view, int position) {
                adapter.setSelection(position);
                execute(position);
                Toast.makeText(context, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        });
        /* 该接口针对Item内控件 可传递position参数*/
        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.img) {

                }
            }
        });
    }

    private void execute(final int position) {
        handler = new Handler(getMainLooper());
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        runInBackground(new Runnable() {
            @Override
            public void run() {
                croppedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.beauty_1);
                croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, desiredSize, desiredSize, false);
                imageView.setImageBitmap(croppedBitmap);
                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final long startTime = SystemClock.uptimeMillis();
                stylizeImage(croppedBitmap, position);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                textureCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                done();
            }
        });
    }

    private void done() {
        imageView.setImageBitmap(textureCopyBitmap);
    }

    private int desiredSize = 512;
    private final float[] styleVals = new float[NUM_STYLES];
    private int[] intValues = new int[desiredSize * desiredSize];
    private float[] floatValues = new float[desiredSize * desiredSize * 3];
    private void stylizeImage(Bitmap bitmap, int model) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        for (int i = 0; i < NUM_STYLES; ++i) {
            styleVals[i] = 0f;
        }
        styleVals[model] = 1f;

        // Copy the input data into TensorFlow.
        Log.d("tensor", "Width: " + bitmap.getWidth() + ", Height: " + bitmap.getHeight());
        inferenceInterface.feed(
                INPUT_NODE, floatValues, 1, bitmap.getWidth(), bitmap.getHeight(), 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        inferenceInterface.run(new String[]{OUTPUT_NODE}, false);
        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }

        bitmap.setPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}
