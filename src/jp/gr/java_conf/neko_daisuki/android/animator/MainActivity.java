package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.devsmart.android.ui.HorizontalListView;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.NexecClient;

public class MainActivity extends Activity {

    private class OnFinishListener implements NexecClient.OnFinishListener {

        public void onFinish() {
            showToast("Finished.");
        }
    }

    private class OnGetLineListener implements NexecClient.OnGetLineListener {

        public void onGetLine(String s) {
            showToast(s.trim());
        }
    }

    private static class ActivityResultDispatcher {

        public interface Proc {

            public void run(Intent data);
        }

        private static class FakeProc implements Proc {

            public void run(Intent data) {
            }
        }

        private static class ActivityResult {

            public int requestCode;
            public int resultCode;

            public ActivityResult(int requestCode, int resultCode) {
                this.requestCode = requestCode;
                this.resultCode = resultCode;
            }

            public boolean equals(Object o) {
                ActivityResult result;
                try {
                    result = (ActivityResult)o;
                }
                catch (ClassCastException e) {
                    return false;
                }
                return (result.requestCode == this.requestCode)
                    && (result.resultCode == this.resultCode);
            }

            public int hashCode() {
                Integer n = Integer.valueOf(requestCode);
                Integer m = Integer.valueOf(resultCode);
                return n.hashCode() + m.hashCode();
            }
        }

        private static Proc mFakeProc = new FakeProc();
        private Map<ActivityResult, Proc> mMap;

        public ActivityResultDispatcher() {
            mMap = new HashMap<ActivityResult, Proc>();
        }

        public void put(int requestCode, int resultCode, Proc proc) {
            mMap.put(new ActivityResult(requestCode, resultCode), proc);
        }

        public void dispatch(int requestCode, int resultCode, Intent data) {
            getProc(requestCode, resultCode).run(data);
        }

        private Proc getProc(int requestCode, int resultCode) {
            Proc proc = mMap.get(new ActivityResult(requestCode, resultCode));
            return proc != null ? proc : mFakeProc;
        }
    }

    private class OnConfirmOk implements ActivityResultDispatcher.Proc {

        public void run(Intent data) {
            mNexecClient.execute(data);
        }
    }

    private class Adapter extends BaseAdapter {

        public int getCount() {
            return mFrames.size();
        }

		public View getView(int position, View convertView, ViewGroup parent) {
            String service = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater inflater = (LayoutInflater)getSystemService(service);
            View view = inflater.inflate(R.layout.list_item, parent, false);

            ImageView img = (ImageView)view.findViewById(R.id.image);
            String path = getThumbnailFilePath(mFrames.get(position));
            img.setImageBitmap(BitmapFactory.decodeFile(path));

            return view;
        }

        public long getItemId(int position) {
            return position;
        }

        public Object getItem(int position) {
            return mFrames.get(position);
        }
    }

    private class JpegCallback implements PictureCallback {

        private DateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        public void onPictureTaken(byte[] data, Camera camera) {
            String fileId = mDateFormat.format(new Date());

            String originalPath = getOriginalFilePath(fileId);
            try {
                saveFile(originalPath, data);

                String thumbnailPath = getThumbnailFilePath(fileId);
                saveThumbnail(originalPath, thumbnailPath);
            }
            catch (IOException e) {
                showException("failed to save", e);
                return;
            }

            mFrames.add(fileId);
        }

        private void saveThumbnail(String originalPath, String thumbnailPath) throws IOException {
            Bitmap bmp = BitmapFactory.decodeFile(originalPath);
            Bitmap thumb = Bitmap.createScaledBitmap(bmp, 320, 240, false);
            OutputStream out = new FileOutputStream(thumbnailPath);
            try {
                thumb.compress(CompressFormat.JPEG, 100, out);
            }
            finally {
                out.close();
            }
        }

        private void saveFile(String path, byte[] data) throws IOException {
            OutputStream out = new FileOutputStream(path);
            try {
                out.write(data);
            }
            finally {
                out.close();
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
            return candidates.size() == 0
                ? sizes.get(0)
                : findLargestSize(candidates);
        }

        private Camera.Size findLargestSize(List<Camera.Size> sizes) {
            Camera.Size largestSize = sizes.get(0);
            int largestArea = largestSize.width * largestSize.height;
            int len = sizes.size();
            for (int i = 1; i < len; i++) {
                Camera.Size size = sizes.get(i);
                int area = size.width * size.height;
                boolean isSmall = area < largestArea;
                largestSize = isSmall ? largestSize : size;
                largestArea = isSmall ? largestArea : area;
            }
            return largestSize;
        }

        private List<Camera.Size> dropTooLargePreviewSizes(int width, int height, List<Camera.Size> sizes) {
            List<Camera.Size> l = new LinkedList<Camera.Size>();
            for (Camera.Size size: sizes) {
                Camera.Size[] a =
                    (width < size.width) || (height < size.height)
                    ? new Camera.Size[0]
                    : new Camera.Size[] { size };
                l.addAll(Arrays.asList(a));
            }

            return l;
        }
    }

    private static final String TAG = "animator";
    private static final int REQUEST_CONFIRM = 0;

    // Document
    private String mProjectDirectory;
    private List<String> mFrames = new ArrayList<String>();

    // View
    private SurfaceView mView;

    // Helper
    private Camera mCamera;
    private PictureCallback mJpegCallback = new JpegCallback();
    private NexecClient mNexecClient;
    private ActivityResultDispatcher mActivityResultDispatcher;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        NexecClient.Settings settings = new NexecClient.Settings();
        settings.host = "192.168.11.8";
        settings.port = 57005;
        settings.args = buildArgs();
        settings.files = listFiles();
        mNexecClient.request(settings, REQUEST_CONFIRM);
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

        mNexecClient = new NexecClient(this);
        OnGetLineListener outListener = new OnGetLineListener();
        mNexecClient.setStdoutOnGetLineListener(outListener);
        mNexecClient.setStderrOnGetLineListener(outListener);
        mNexecClient.setOnFinishListener(new OnFinishListener());

        mActivityResultDispatcher = new ActivityResultDispatcher();
        mActivityResultDispatcher.put(
                REQUEST_CONFIRM, RESULT_OK, new OnConfirmOk());

        File parentDirectory = Environment.getExternalStorageDirectory();
        String absoluteParentDirectory = parentDirectory.getAbsolutePath();
        String fmt = "%s/.animator/default";
        mProjectDirectory = String.format(fmt, absoluteParentDirectory);
        new File(mProjectDirectory).mkdirs();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultDispatcher.dispatch(requestCode, resultCode, data);
    }

    protected void onResume() {
        super.onResume();

        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(new SurfaceListener());
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();
    }

    protected void onPause() {
        super.onPause();

        mCamera.stopPreview();
        mCamera.release();
    }

    private void showException(String msg, Throwable e) {
        e.printStackTrace();
        showToast(String.format("%s: %s", msg, e.getMessage()));
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private String getThumbnailFilePath(String id) {
        return String.format("%s/%s-thumbnail.jpg", mProjectDirectory, id);
    }

    private String getOriginalFilePath(String id) {
        return String.format("%s/%s-original.jpg", mProjectDirectory, id);
    }

    private String getDestinationPath() {
        return String.format("%s/movie.avi", mProjectDirectory);
    }

    private String[] listFiles() {
        List<String> files = new LinkedList<String>();

        for (String id: mFrames) {
            files.add(getOriginalFilePath(id));
        }
        files.add(getDestinationPath());

        return files.toArray(new String[0]);
    }

    private String[] buildArgs() {
        List<String> args = new LinkedList<String>();

        args.add("ffmpeg");
        args.add("-loglevel");
        args.add("quiet");
        args.add("-y");
        args.add("-r");
        args.add("1");
        args.add("-f");
        args.add("image2");
        for (String id: mFrames) {
            args.add("-i");
            args.add(getOriginalFilePath(id));
        }
        args.add(getDestinationPath());

        return args.toArray(new String[0]);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
