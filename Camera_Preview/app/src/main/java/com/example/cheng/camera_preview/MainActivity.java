package com.example.cheng.camera_preview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

import static android.hardware.Camera.getNumberOfCameras;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "My_CAMERA";
    Camera myCamera;
    SurfaceView previewSurfaceView;
    SurfaceHolder previewSurfaceHolder;
    boolean previewing = false;
    ImageView ImgView;
    private StreamIt streamIt = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewSurfaceView = (SurfaceView) findViewById(R.id.previewsurface);
        previewSurfaceHolder = previewSurfaceView.getHolder();// 绑定SurfaceView，取得SurfaceHolder
        previewSurfaceHolder.addCallback(this);//SurfaceHolder加入回调接口
        previewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 設置顯示器類型，setType必须设置
        ImgView = (ImageView) findViewById(R.id.ImgView);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // TODO Auto-generated method stub
        if (previewing) {//如果硬體沒有相機
            myCamera.stopPreview();//停止相機功能
            previewing = false;
        }
        myCamera = Camera.open();//開啟鏡頭（2.3版本后支持多摄像头,需传入参数）
        int a = getNumberOfCameras();
        Log.d(TAG + " number = ", String.valueOf(a));//硬體鏡頭可用數

        try {///設置surface用來顯示實時的預覽
            myCamera.setPreviewDisplay(surfaceHolder);
            Log.d(TAG, "start");
            previewing = true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = myCamera.getParameters();//獲取Camera.Parameters的實例
        myCamera.startPreview();
        Log.d(TAG, format + "+" + width + "+" + height);

        //Camera Focus mode 必須透過 setParameters() 設定，自動對焦則有三種模式
        List<String> allFocus = parameters.getSupportedFocusModes();
        if (allFocus.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//设置聚焦模式
        } else if (allFocus.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);//设置聚焦模式
        }
        streamIt = new StreamIt();
        parameters.setPreviewSize(640, 480);//w,h
        parameters.setPictureSize(640, 480);
//        myCamera.setDisplayOrientation(90);//螢幕轉向90度
        myCamera.setPreviewCallback(streamIt);
        myCamera.setParameters(parameters);// 设置Camera parameters
        myCamera.startPreview();//開始捕獲並顯示幀
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        myCamera.stopPreview();//停止相機功能
        myCamera.release();
        myCamera = null;
        previewing = false;
    }
    class StreamIt implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            Log.i(TAG, "执行了capture方法");
            Bitmap bitmap = NV21_TO_RGB(data, 640, 480);//NV21 RGB
            ImgView.setImageBitmap(bitmap);
        }
    }
    public Bitmap NV21_TO_RGB(byte[] data, int width, int height) {
        final int frameSize = width * height;
        int[] rgb = new int[width * height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) data[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & data[uvp++]) - 128;
                    u = (0xff & data[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
    }
}
