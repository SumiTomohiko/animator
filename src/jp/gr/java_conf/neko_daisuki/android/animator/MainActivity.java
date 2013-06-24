package jp.gr.java_conf.neko_daisuki.android.animator;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity {

    private class HolderListener implements SurfaceHolder.Callback {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(mView.getHolder());
            }
            catch (Exception e) {
                e.printStackTrace();
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

        mView = (SurfaceView)findViewById(R.id.preview);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(new HolderListener());
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();
    }

    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }

    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
