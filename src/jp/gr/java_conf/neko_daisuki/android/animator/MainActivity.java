package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.devsmart.android.ui.HorizontalListView;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.NexecClient;

public class MainActivity extends Activity {

    private class SelectProjectDialogOnClickListener implements DialogInterface.OnClickListener {

        private String[] mProjects;

        public SelectProjectDialogOnClickListener(String[] projects) {
            mProjects = projects;
        }

        public void onClick(DialogInterface dialog, int id) {
            writeProject();
            changeProject(mProjects[id]);
        }
    }

    private class RenameProjectDialogOkButtonOnClickListener implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int id) {
            File src = new File(mProjectDirectory);
            String name = mRenameProjectDialogViews.text.getText().toString();
            String path = getProjectDirectory(name);
            src.renameTo(new File(path));
            mProjectDirectory = path;
        }
    }

    private class CreateProjectDialogOkButtonOnClickListener implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int id) {
            writeProject();
            changeProject(mCreateProjectDialogViews.text.getText().toString());
        }
    }

    private interface DialogCreator {

        public Dialog create();
    }

    private class SelectProjectDialogCreator implements DialogCreator {

        public Dialog create() {
            String[] projects = listProjects();

            Builder builder = new Builder(MainActivity.this);
            builder.setItems(
                    projects, new SelectProjectDialogOnClickListener(projects));
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }

        private String[] listProjects() {
            List<String> projects = new LinkedList<String>();
            for (File file: new File(getApplicationDirectory()).listFiles()) {
                String[] a = file.isDirectory()
                    ? new String[] { file.getName() } : new String[0];
                projects.addAll(Arrays.asList(a));
            }
            return projects.toArray(new String[0]);
        }
    }

    private abstract class ProjectNameDialogCreator implements DialogCreator {

        public Dialog create() {
            Builder builder = new Builder(MainActivity.this);
            builder.setView(getView());
            builder.setPositiveButton("Okey", getOnClickListener());
            builder.setNegativeButton("Cancel", null);
            return builder.create();
        }

        protected abstract View getView();
        protected abstract DialogInterface.OnClickListener getOnClickListener();
    }

    private class CreateProjectDialogCreator extends ProjectNameDialogCreator {

        protected View getView() {
            return mCreateProjectDialogViews.dialog;
        }

        protected DialogInterface.OnClickListener getOnClickListener() {
            return new CreateProjectDialogOkButtonOnClickListener();
        }
    }

    private class RenameProjectDialogCreator extends ProjectNameDialogCreator {

        protected View getView() {
            return mRenameProjectDialogViews.dialog;
        }

        protected DialogInterface.OnClickListener getOnClickListener() {
            return new RenameProjectDialogOkButtonOnClickListener();
        }
    }

    private interface MenuAction {

        public void run();
    }

    private class RenameProjectAction implements MenuAction {

        public void run() {
            mRenameProjectDialogViews.text.setText(getProjectName());
            showDialog(DIALOG_RENAME_PROJECT);
        }
    }

    private class CreateProjectAction implements MenuAction {

        public void run() {
            showDialog(DIALOG_CREATE_PROJECT);
        }
    }

    private class SelectProjectAction implements MenuAction {

        public void run() {
            showDialog(DIALOG_SELECT_PROJECT);
        }
    }

    private class ClearProjectAction implements MenuAction {

        public void run() {
            File directory = new File(mProjectDirectory);
            for (File file: directory.listFiles()) {
                file.delete();
            }
        }
    }

    private class MakeMovieAction implements MenuAction {

        public void run() {
            NexecClient.Settings settings = new NexecClient.Settings();
            settings.host = "192.168.11.8";
            settings.port = 57005;
            settings.args = buildArgs();
            settings.files = listFiles();
            addLinks(settings);
            mNexecClient.request(settings, REQUEST_CONFIRM);
        }
    }

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

    private static class ProjectNameDialogViews {

        public View dialog;
        public EditText text;
    }

    private static class FrameRate {

        private int mRate;

        public FrameRate(int rate) {
            mRate = rate;
        }

        public String toString() {
            return Integer.toString(mRate);
        }
    }

    private static final String TAG = "animator";
    private static final int REQUEST_CONFIRM = 0;
    private static final int DIALOG_CREATE_PROJECT = 0;
    private static final int DIALOG_RENAME_PROJECT = 1;
    private static final int DIALOG_SELECT_PROJECT = 2;

    // Document
    private String mProjectDirectory;
    private List<String> mFrames = new ArrayList<String>();
    private FrameRate mFrameRate = new FrameRate(8);

    // View
    private SurfaceView mView;
    private ProjectNameDialogViews mCreateProjectDialogViews;
    private ProjectNameDialogViews mRenameProjectDialogViews;

    // Helper
    private Camera mCamera;
    private PictureCallback mJpegCallback = new JpegCallback();
    private NexecClient mNexecClient;
    private ActivityResultDispatcher mActivityResultDispatcher;
    private SparseArray<MenuAction> mMenuActions;
    private SparseArray<DialogCreator> mDialogCreators;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        mMenuActions.get(item.getItemId()).run();
        return true;
    }

    protected Dialog onCreateDialog(int id) {
        return mDialogCreators.get(id).create();
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

        mMenuActions = new SparseArray<MenuAction>();
        mMenuActions.put(R.id.action_create_project, new CreateProjectAction());
        mMenuActions.put(R.id.action_rename_project, new RenameProjectAction());
        mMenuActions.put(R.id.action_clear_project, new ClearProjectAction());
        mMenuActions.put(R.id.action_select_project, new SelectProjectAction());
        mMenuActions.put(R.id.action_make_movie, new MakeMovieAction());

        mDialogCreators = new SparseArray<DialogCreator>();
        mDialogCreators.put(
                DIALOG_CREATE_PROJECT, new CreateProjectDialogCreator());
        mDialogCreators.put(
                DIALOG_RENAME_PROJECT, new RenameProjectDialogCreator());
        mDialogCreators.put(
                DIALOG_SELECT_PROJECT, new SelectProjectDialogCreator());

        mCreateProjectDialogViews = createProjectNameDialog();
        mRenameProjectDialogViews = createProjectNameDialog();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultDispatcher.dispatch(requestCode, resultCode, data);
    }

    protected void onResume() {
        super.onResume();

        String defaultName = readDefaultProjectName();
        String projectName = defaultName != null ? defaultName : "default";
        changeProject(projectName);

        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(new SurfaceListener());
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();
    }

    protected void onPause() {
        super.onPause();

        mCamera.stopPreview();
        mCamera.release();

        writeProject();
        writeDefaultProjectName();
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
        return String.format("%s/movie.mp4", mProjectDirectory);
    }

    private void addLink(NexecClient.Settings settings, String id, int n) {
        String dest = getOriginalFilePath(id);
        String src = String.format("%s/%d.jpg", mProjectDirectory, n);
        settings.addLink(dest, src);
    }

    private void addLinks(NexecClient.Settings settings) {
        addLink(settings, mFrames.get(0), 1);

        int len = mFrames.size();
        for (int i = 0; i < len; i++) {
            addLink(settings, mFrames.get(i), i + 2);
        }
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
        return new String[] {
            "ffmpeg", "-loglevel", "quiet", "-y", "-r", mFrameRate.toString(),
            "-f", "image2", "-i",
            String.format("%s/%%d.jpg", mProjectDirectory), "-r", "24", "-s",
            "xga", getDestinationPath() };
    }

    private View inflateProjectNameDialog() {
        return getLayoutInflater().inflate(R.layout.dialog_project_name, null);
    }

    private ProjectNameDialogViews createProjectNameDialog() {
        ProjectNameDialogViews views = new ProjectNameDialogViews();

        views.dialog = inflateProjectNameDialog();
        views.text = (EditText)views.dialog.findViewById(R.id.name);

        return views;
    }

    private String getApplicationDirectory() {
        File parentDirectory = Environment.getExternalStorageDirectory();
        String absoluteParentDirectory = parentDirectory.getAbsolutePath();
        return String.format("%s/.animator", absoluteParentDirectory);
    }

    private String getProjectDirectory(String name) {
        return String.format("%s/%s", getApplicationDirectory(), name);
    }

    private String readDefaultProjectName() {
        String path = getDefaultJsonPath();
        Reader fileReader;
        try {
            fileReader = new FileReader(path);
        }
        catch (FileNotFoundException e) {
            return null;
        }
        try {
            JsonReader reader = new JsonReader(fileReader);
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("name")) {
                        return reader.nextString();
                    }
                }
                reader.endObject();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            showException(String.format("failed to read %s", path), e);
        }
        return null;
    }

    private void changeProject(String name) {
        mProjectDirectory = getProjectDirectory(name);

        /*
         * When a user changes the project, the following statement is unneeded.
         * But it is harmless, too. So I placed it here to simplify the code.
         */
        new File(mProjectDirectory).mkdirs();

        mFrames.clear();

        String path = getProjectJsonPath();
        Reader fileReader;
        try {
            fileReader = new FileReader(path);
        }
        catch (FileNotFoundException e) {
            return;
        }
        try {
            JsonReader reader = new JsonReader(fileReader);
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (key.equals("frames")) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            mFrames.add(reader.nextString());
                        }
                        reader.endArray();
                    }
                }
                reader.endObject();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            showException(String.format("failed to read %s", path), e);
        }
    }

    private String getProjectJsonPath() {
        return String.format("%s/project.json", mProjectDirectory);
    }

    private void writeProject() {
        String path = getProjectJsonPath();
        try {
            JsonWriter writer = new JsonWriter(new FileWriter(path));
            try {
                writer.beginObject();
                writer.name("frames");
                writer.beginArray();
                for (String id: mFrames) {
                    writer.value(id);
                }
                writer.endArray();
                writer.endObject();
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            showException(String.format("failed to write %s", path), e);
        }
    }

    private String getDefaultJsonPath() {
        return String.format("%s/default.json", getApplicationDirectory());
    }

    private void writeDefaultProjectName() {
        String path = getDefaultJsonPath();
        try {
            JsonWriter writer = new JsonWriter(new FileWriter(path));
            try {
                writer.beginObject();
                writer.name("name").value(getProjectName());
                writer.endObject();
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            showException(String.format("failed to write %s", path), e);
        }
    }

    private String getProjectName() {
        return new File(mProjectDirectory).getName();
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
