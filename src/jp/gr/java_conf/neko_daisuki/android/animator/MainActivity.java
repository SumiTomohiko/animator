package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.devsmart.android.ui.HorizontalListView;

public class MainActivity extends Activity {

    private class Adapter extends BaseAdapter {

        public int getCount() {
            return mFrames.size();
        }

		public View getView(int position, View convertView, ViewGroup parent) {
            String service = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater inflater = (LayoutInflater)getSystemService(service);
            return inflater.inflate(R.layout.list_item, parent, false);
        }

        public long getItemId(int position) {
            return position;
        }

        public Object getItem(int position) {
            return mFrames.get(position);
        }
    }

    private class JpegCallback implements PictureCallback {

        public void onPictureTaken(byte[] data, Camera camera) {
            File file = new File(mProjectDirectory, "animator.jpg");
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
                    mFrames.add(file.getName());
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

    private class SurfaceListener implements SurfaceHolder.Callback {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(mView.getHolder());
            }
            catch (IOException e) {
                showException("failed to show preview", e);
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            String fmt = "the surface was changed: width=%d, height=%d.";
            Log.d(TAG, String.format(fmt, width, height));

            mCamera.stopPreview();
            Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            Camera.Size bestSize = findBestPreviewSize(width, height, sizes);
            shrinkView(mView, bestSize);

            params.setPreviewSize(bestSize.width, bestSize.height);
            mCamera.setParameters(params);
            mCamera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        private void shrinkView(View view, Camera.Size size) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = size.width;
            params.height = size.height;
            view.setLayoutParams(params);
        }

        private Camera.Size findBestPreviewSize(int width, int height, List<Camera.Size> sizes) {
            List<Camera.Size> candidates = dropTooLargePreviewSizes(width, height, sizes);
            if (candidates.size() == 0) {
                return sizes.get(0);
            }
            Camera.Size largestSize = candidates.get(0);
            int largestArea = largestSize.width * largestSize.height;
            int len = candidates.size();
            for (int i = 1; i < len; i++) {
                Camera.Size size = candidates.get(i);
                int area = size.width * size.height;
                if (area < largestArea) {
                    continue;
                }
                largestSize = size;
                largestArea = area;
            }
            return largestSize;
        }

        private List<Camera.Size> dropTooLargePreviewSizes(int width, int height, List<Camera.Size> sizes) {
            String fmt = "preview size of width=%d, heigh=%d is %s.";

            List<Camera.Size> l = new LinkedList<Camera.Size>();
            for (Camera.Size size: sizes) {
                if ((width < size.width) || (height < size.height)) {
                    String msg = String.format(
                            fmt, size.width, size.height, "too large");
                    Log.d(TAG, msg);
                    continue;
                }
                String msg = String.format(
                        fmt, size.width, size.height, "enough small");
                Log.d(TAG, msg);

                l.add(size);
            }

            return l;
        }
    }

    private static final String TAG = "animator";

    // Document
    private String mProjectDirectory;
    private List<String> mFrames = new ArrayList<String>();

    // View
    private SurfaceView mView;

    // Helper
    private Camera mCamera;
    private PictureCallback mJpegCallback = new JpegCallback();

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
        HorizontalListView list = (HorizontalListView)findViewById(R.id.list);
        list.setAdapter(new Adapter());

        mView = (SurfaceView)findViewById(R.id.preview);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(new SurfaceListener());
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();

        File parentDirectory = Environment.getExternalStorageDirectory();
        String absoluteParentDirectory = parentDirectory.getAbsolutePath();
        String fmt = "%s/.animator/default";
        mProjectDirectory = String.format(fmt, absoluteParentDirectory);
        new File(mProjectDirectory).mkdirs();
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
