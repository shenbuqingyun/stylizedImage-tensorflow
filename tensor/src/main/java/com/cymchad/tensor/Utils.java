package com.cymchad.tensor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * UI Utility functions
 */
public class Utils {
    public static final String TAG = "Utils:";

    private static final String SAVE_FOLDER = "PencilCamera";
    private static final String SAVE_FILENAME_PREFIX = "IMG";

    private static int lastSaveFileIndex = 0;

    /**
     * 混合两张Bitmap 返回融合后的Bitmap
     *
     * @param src 主图
     * @param dst 修饰图
     */
    public static Bitmap xFerMode(Bitmap src, Bitmap dst) {
        Bitmap lightenModeBitmap = Bitmap.createBitmap(dst.getWidth(), dst.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(lightenModeBitmap);
        Paint paint1 = new Paint();
        paint1.setAntiAlias(true);
        Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawBitmap(src, srcRect, srcRect, paint1); //画人物 src
        paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
        // 再把原来的bitmap画到现在的bitmap
        canvas.drawBitmap(dst, srcRect, srcRect, paint1); //画特效 dst
        return lightenModeBitmap;
    }

    /**
     * 图片透明度处理
     *
     * @param sourceImg 原始图片
     * @param number    透明度
     * @return
     */
    public static Bitmap setAlpha(Bitmap sourceImg, int number) {
            try {
                int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];
                sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0,
                        sourceImg.getWidth(), sourceImg.getHeight());// 获得图片的ARGB值
                number = number * 255 / 100;
                for (int i = 0; i < argb.length; i++) {
                    if ((argb[i] & 0xff000000) != 0x00000000) {// 透明色不做处理
                        argb[i] = (number << 24) | (argb[i] & 0xFFFFFF);// 修改最高2位的值
                    }
                }
                sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(),
                        sourceImg.getHeight(), Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                System.gc();
            }
            return sourceImg;
    }

    /**
     * Return the compressed bitmap using sample size.
     *
     * @param src       The source of bitmap.
     * @param maxWidth  The maximum width.
     * @param maxHeight The maximum height.
     * @param recycle   True to recycle the source of bitmap, false otherwise.
     * @return the compressed bitmap
     */
    public static Bitmap compressBySampleSize(final Bitmap src,
                                              final int maxWidth,
                                              final int maxHeight,
                                              final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        if (recycle && !src.isRecycled()) src.recycle();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    /**
     * Return the sample size.
     * 1500*1500的大图采样率为8 尺寸为562*562
     * 300*400的采样率为2
     * @param options   The options.
     * @param maxWidth  The maximum width.
     * @param maxHeight The maximum height.
     * @return the sample size
     */
    private static int calculateInSampleSize(final BitmapFactory.Options options,
                                             final int maxWidth,
                                             final int maxHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while (height > maxHeight || width > maxWidth) {
            height >>= 1;
            width >>= 1;
            inSampleSize <<= 1;
        }
        return inSampleSize;
    }

    private static boolean isEmptyBitmap(final Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }

    /**
     * Reize bitmap with dimensions equal to or less than given params, without changing the aspect ratio
     * @param bitmap    Input bitmap
     * @param maxWidth  Max allowed width of resized Bitmap
     * @param maxHeight Max allowed height of resized Bitmap
     * @return Resized bitmap
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
            return scaledBitmap;
        } else {
            return bitmap;
        }
    }

    /**
     * Save bitmap as JPEG with a incremental filename, in the SAVE_FOLDER directory
     * @param activity
     * @param bitmap
     * @return
     * @throws IOException
     */
    public static String saveBitmap(Activity activity, File mFile,Bitmap bitmap) throws IOException {
        File file;
        try {
            // Create a new save file
            file = mFile;
            OutputStream fOut = new FileOutputStream(file);
            // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.close();

            // Add saved file to gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            activity.sendBroadcast(mediaScanIntent);

        } catch(IOException e) {
            String errorMsg = "Unable to save image";
            Log.e(TAG, errorMsg + ":" + e.getMessage());
            throw new IOException(errorMsg);
        }
        return file.getPath();
    }

    /**
     * 按比例改变图片（单位像素） lessen the bitmap
     * 输入目标大小即可
     *
     * @param destWidth  the dest bitmap width
     * @param destHeigth
     * @return new bitmap if successful ,otherwise null
     */
    public static Bitmap lessenBitmap(Bitmap bitmap, int destWidth, int destHeigth) {

        final BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;

        final int w = bitmap.getWidth();// 源文件的大小
        final int h = bitmap.getHeight();
        float scaleWidth = (float) destWidth / w;// 宽度缩小比例
        float scaleHeight = (float) destHeigth / h;// 高度缩小比例
        final Matrix m = new Matrix();// 矩阵
        m.postScale(scaleWidth, scaleHeight);// 设置矩阵比例
        // 直接按照矩阵的比例把源文件画入
        final Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
//        if (!bitmap.isRecycled()) {
//            bitmap.recycle();
//        }
        return resizedBitmap;
    }

    /**
     * 将bitmap缩放到 newWidth x newHeight 大小
     */
    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        // newWidth 和 newHeight不能小于0
        if (newWidth <= 0) {
            newWidth = 1;
        }
        if (newHeight <= 0) {
            newHeight = 1;
        }

        int width = bm.getWidth();
        int height = bm.getHeight();
        //计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    /**
     * Rotate bitmap by given angle in degrees
     * @param bitmap
     * @param angle
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Create a new save file in the SAVE_FOLDER directory with name as '<SAVE_FILENAME_PREFIX>_<3 digit serial number>'
     * ( example: IMG_001)
     * @return
     * @throws IOException
     */
    private static File createSaveFile() throws IOException {
        File file;
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        String saveFolderPath = path + '/' + SAVE_FOLDER; // PencilCamera
        int saveFileIndex = lastSaveFileIndex;
        do {
            String saveFileName = SAVE_FILENAME_PREFIX + String.format("%03d", ++saveFileIndex) + ".jpg"; // IMG002.jpg
            String saveFilePath = saveFolderPath + '/' + saveFileName;
            Log.d(TAG,"Saving image to path - "+saveFilePath);
            file = new File(saveFilePath); // the File to save to
            file.getParentFile().mkdirs();
        } while(file.exists());
        file.createNewFile();
        return file;
    }
}
