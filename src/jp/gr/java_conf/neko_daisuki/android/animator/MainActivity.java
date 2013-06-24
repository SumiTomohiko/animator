package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    private class JpegCallback implements PictureCallback {

        public void onPictureTaken(byte[] data, Camera camera) {
            File file = new File(
                    Environment.getExternalStorageDirectory(),
                    "animator.jpg");
            OutputStream out;
            try {
                out = new FileOutputStream(file);
                try {
                    try {
                        out.write(data);
                    }
                    finally {
                        out.close();
                    }
                }
                catch (IOException e) {
                    showException("failed to write JPEG data", e);
                }
            }
            catch (FileNotFoundException e) {
                showException("failed to open a file", e);
            }
        }
    }

    private class ShotButtonOnClickListener implements OnClickListener {

        public void onClick(View view) {
            mCamera.takePicture(null, null, mJpegCallback);
        }
    }

    private class HolderListener implements SurfaceHolder.Callback {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(mView.getHolder());
            }
            catch (IOException e) {
                showException("failed to show preview", e);
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCamera.stopPreview();

            mCamera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    }

    private Camera mCamera;
    private PictureCallback mJpegCallback;
    private SurfaceView mView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View shotButton = findViewById(R.id.shot_button);
        shotButton.setOnClickListener(new ShotButtonOnClickListener());

        mView = (SurfaceView)findViewById(R.id.preview);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(new HolderListener());
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();
        mJpegCallback = new JpegCallback();
    }

    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }

    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    private void showException(String msg, Throwable e) {
        e.printStackTrace();
        String s = String.format("%s: %s", msg, e.getMessage());
        Toast.makeText(this, s, Toast.LENGTH_LONG);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
