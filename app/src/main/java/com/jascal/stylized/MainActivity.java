package com.jascal.stylized;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ScreenUtils;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String MODEL_FILE = "stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final int NUM_STYLES = 26;
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";

    private TensorFlowInferenceInterface inferenceInterface;
    private Handler handler;

    private long lastProcessingTimeMs;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap textureCopyBitmap;

    private ImageView imageView, reultImg;
    private int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "desiredSize: " + desiredSize);
        imageView = findViewById(R.id.origin);
        reultImg = findViewById(R.id.result);
        croppedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.beauty);
        croppedBitmap = reSizeBitmap(croppedBitmap);
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, desiredSize, desiredSize, false);
        imageView.setImageBitmap(croppedBitmap);
    }

    // 这个方法就是将一张长方形图片转换成一张正方向图片
    private Bitmap reSizeBitmap(Bitmap bitmap) {
        Bitmap b = null;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        if (width < height) {
            b = Bitmap.createBitmap(height, height, bitmap.getConfig());
            Canvas c = new Canvas(b);
            c.drawBitmap(bitmap, (height - width) / 2, 0, null);
        } else if (height < width) {
            b = Bitmap.createBitmap(width, width, bitmap.getConfig());
            Canvas c = new Canvas(b);
            c.drawBitmap(bitmap, 0, (width - height) / 2, null);
        } else if (width == height) {
            return bitmap;
        }
        return b;
    }

    public void go(View view) {
        execute();
    }

    private void execute() {
        handler = new Handler(getMainLooper());
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        runInBackground(new Runnable() {
            @Override
            public void run() {
                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final long startTime = SystemClock.uptimeMillis();
                stylizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                textureCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                done();
            }
        });
    }

    private void done() {
        if (width < height) {
            float scale = desiredSize * 1.0f / height * 1.0f;
            Bitmap bitmap = ImageUtils.clip(textureCopyBitmap, (desiredSize - Math.round(width * scale)) / 2, 0, Math.round(width * scale), desiredSize);
            reultImg.setImageBitmap(bitmap);
        } else {
            float scale = desiredSize * 1.0f / width * 1.0f;
            Bitmap bitmap = ImageUtils.clip(textureCopyBitmap, 0, (desiredSize - Math.round(height * scale)) / 2, desiredSize, Math.round(height * scale));
            reultImg.setImageBitmap(bitmap);
        }
    }

    private int desiredSize = ScreenUtils.getScreenWidth();
    private final float[] styleVals = new float[NUM_STYLES];
    private int[] intValues = new int[desiredSize * desiredSize];
    private float[] floatValues = new float[desiredSize * desiredSize * 3];

    private void stylizeImage(Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        float style;
        for (int i = 0; i < NUM_STYLES; ++i) {
            style = (float) Math.random();

            styleVals[i] = style / NUM_STYLES;
        }

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
