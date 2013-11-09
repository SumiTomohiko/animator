package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import jp.gr.java_conf.neko_daisuki.android.animator.widget.FocusAreaView;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.NexecClient;

public class MainActivity extends FragmentActivity {

    public static class ClearProjectDialog extends DialogFragment {

        private class OkeyButtonOnClickListener implements DialogInterface.OnClickListener {

            public void onClick(DialogInterface dialog, int which) {
                for (File file: new File(mDirectory).listFiles()) {
                    file.delete();
                }
                MainActivity activity = (MainActivity)getActivity();
                activity.clearFrames();
            }
        }

        private static final String KEY_DIRECTORY = "directory";

        private String mDirectory;

        public static ClearProjectDialog newInstance(String directory) {
            ClearProjectDialog dialog = new ClearProjectDialog();
            Bundle args = new Bundle();
            args.putString(KEY_DIRECTORY, directory);
            dialog.setArguments(args);
            return dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            Builder builder = new Builder(activity);
            mDirectory = getArguments().getString(KEY_DIRECTORY);
            builder.setMessage(R.string.dialog_clear_project);
            builder.setPositiveButton("Okey", new OkeyButtonOnClickListener());
            builder.setNegativeButton("Cancel", null);

            return builder.create();
        }
    }

    public static class SelectProjectDialog extends DialogFragment {

        private class ListOnClickListener implements DialogInterface.OnClickListener {

            public void onClick(DialogInterface dialog, int id) {
                MainActivity activity = (MainActivity)getActivity();
                activity.writeProject();
                activity.changeProject(mProjects[id]);
            }
        }

        private static final String KEY_PROJECTS = "projects";

        private String[] mProjects;

        public static DialogFragment newInstance(String[] projects) {
            SelectProjectDialog dialog = new SelectProjectDialog();
            Bundle args = new Bundle();
            args.putStringArray(KEY_PROJECTS, projects);
            dialog.setArguments(args);
            return dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            Builder builder = new Builder(activity);
            mProjects = getArguments().getStringArray(KEY_PROJECTS);
            builder.setItems(mProjects, new ListOnClickListener());
            builder.setNegativeButton("Cancel", null);

            return builder.create();
        }
    }

    private class FocusButtonListener implements OnClickListener {

        public void onClick(View view) {
            mCamera.autoFocus(null);
        }
    }

    private class FocusAreaListener implements FocusAreaView.OnAreaChangedListener {

        public void onAreaChanged(FocusAreaView view, List<Camera.Area> areas) {
            Parameters params = mCamera.getParameters();
            params.setFocusAreas(areas);
            mCamera.setParameters(params);
            mCamera.autoFocus(null);
        }
    }

    private interface CameraReader {

        public void run();
    }

    private class NopCameraReader implements CameraReader {

        public void run() {
        }
    }

    private class DefaultCameraReader implements CameraReader {

        public void run() {
            mCameraParameters = new CameraParameters(mCamera.getParameters());
            readCameraParameters();
        }
    }

    private abstract static class ProjectNameDialog extends DialogFragment {

        private class OnShowListener implements DialogInterface.OnShowListener {

            private class OkeyButtonOnClickListener implements OnClickListener {

                public void onClick(View view) {
                    MainActivity activity = (MainActivity)getActivity();
                    String name = mNameEdit.getText().toString().trim();
                    if (name.equals("")) {
                        return;
                    }
                    onOkey(activity, name);
                    mDialog.dismiss();
                }
            }

            private AlertDialog mDialog;

            public OnShowListener(AlertDialog dialog) {
                mDialog = dialog;
            }

            public void onShow(DialogInterface dialog) {
                View button = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new OkeyButtonOnClickListener());
            }
        }

        protected static final String KEY_NAME = "name";

        private EditText mNameEdit;

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            Builder builder = new Builder(activity);

            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_project_name, null);
            builder.setView(view);
            mNameEdit = (EditText)view.findViewById(R.id.name);
            mNameEdit.setText(getArguments().getString(KEY_NAME));

            builder.setPositiveButton("Okey", null);
            builder.setNegativeButton("Cancel", null);
            AlertDialog d = builder.create();
            d.setOnShowListener(new OnShowListener(d));

            return d;
        }

        protected abstract void onOkey(MainActivity activity, String name);
    }

    public static class RenameProjectDialog extends ProjectNameDialog {

        public static DialogFragment newInstance(String name) {
            RenameProjectDialog dialog = new RenameProjectDialog();
            Bundle args = new Bundle();
            args.putString(KEY_NAME, name);
            dialog.setArguments(args);

            return dialog;
        }

        protected void onOkey(MainActivity activity, String name) {
            String path = activity.getProjectDirectory(name);
            new File(activity.mProjectDirectory).renameTo(new File(path));
            activity.mProjectDirectory = path;
        }
    }

    public static class CreateProjectDialog extends ProjectNameDialog {

        public static DialogFragment newInstance() {
            CreateProjectDialog dialog = new CreateProjectDialog();
            Bundle args = new Bundle();
            args.putString(KEY_NAME, "");
            dialog.setArguments(args);

            return dialog;
        }

        protected void onOkey(MainActivity activity, String name) {
            activity.writeProject();
            activity.changeProject(name);
        }
    }

    private interface MenuAction {

        public void run();
    }

    private static class NopMenuAction implements MenuAction {

        public void run() {
        }
    }

    private class AboutAction implements MenuAction {

        public void run() {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
        }
    }

    private class RenameProjectAction implements MenuAction {

        public void run() {
            String name = getProjectName();
            DialogFragment dialog = RenameProjectDialog.newInstance(name);
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private class WatchLogAction implements MenuAction {

        public void run() {
            Context ctx = MainActivity.this;
            Intent intent = new Intent(ctx, LogActivity.class);
            intent.putExtra(LogActivity.KEY_LOG_PATH, getLogPath());
            startActivity(intent);
        }
    }

    private class WatchMovieAction implements MenuAction {

        public void run() {
            showFile(getMoviePath(), "video/mp4");
        }
    }

    private class CreateProjectAction implements MenuAction {

        public void run() {
            DialogFragment dialog = CreateProjectDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private class HostSettingsAction implements MenuAction {

        public void run() {
            Context ctx = MainActivity.this;
            Intent intent = new Intent(ctx, HostPreferenceActivity.class);
            intent.putExtra(HostPreferenceActivity.EXTRA_HOST, mHost);
            intent.putExtra(HostPreferenceActivity.EXTRA_PORT, mPort);
            startActivityForResult(intent, REQUEST_HOST_PREFERENCE);
        }
    }

    private class OpenProjectAction implements MenuAction {

        public void run() {
            String[] projects = listProjects();
            DialogFragment dialog = SelectProjectDialog.newInstance(projects);
            dialog.show(getSupportFragmentManager(), "dialog");
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

    private class ClearProjectAction implements MenuAction {

        public void run() {
            String directory = mProjectDirectory;
            DialogFragment dialog = ClearProjectDialog.newInstance(directory);
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private class ProjectSettingsAction implements MenuAction {

        public void run() {
            Context ctx = MainActivity.this;
            Intent intent = new Intent(ctx, ProjectActivity.class);
            intent.putExtra(ProjectActivity.KEY_FPS, mFrameRate.toInteger());
            startActivityForResult(intent, REQUEST_PROJECT_SETTINGS);
        }
    }

    private class CameraSettingsAction implements MenuAction {

        public void run() {
            Context ctx = MainActivity.this;
            Intent intent = new Intent(ctx, CameraActivity.class);
            intent.putExtra(CameraActivity.KEY_ANTIBANDING,
                            mCameraParameters.antibanding);
            intent.putExtra(CameraActivity.KEY_EFFECT,
                            mCameraParameters.effect);
            intent.putExtra(CameraActivity.KEY_FLASH_MODE,
                            mCameraParameters.flashMode);
            intent.putExtra(CameraActivity.KEY_FOCUS_MODE,
                            mCameraParameters.focusMode);
            intent.putExtra(CameraActivity.KEY_SCENE_MODE,
                            mCameraParameters.sceneMode);
            intent.putExtra(CameraActivity.KEY_WHITE_BALANCE,
                            mCameraParameters.whiteBalance);
            startActivityForResult(intent, REQUEST_CAMERA_SETTINGS);
        }
    }

    private class MakeMovieAction implements MenuAction {

        public void run() {
            NexecClient.Settings settings = new NexecClient.Settings();
            settings.host = mHost;
            settings.port = mPort;
            settings.args = buildArgs();
            settings.files = listFiles();
            addLinks(settings);
            mNexecClient.request(settings, REQUEST_CONFIRM);
        }
    }

    private class OnFinishListener implements NexecClient.OnFinishListener {

        public void onFinish() {
            mLogFile.close();
            mLogFile = null;
            ActivityUtil.showToast(MainActivity.this, "Finished.");
        }
    }

    private class OnGetLineListener implements NexecClient.OnGetLineListener {

        public void onGetLine(String s) {
            mLogFile.print(s);
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

    private class OnProjectSettings implements ActivityResultDispatcher.Proc {

        public void run(Intent data) {
            int fps = data.getIntExtra(ProjectActivity.KEY_FPS, 8);
            mFrameRateUpdater = new TrueFrameRateUpdater(new FrameRate(fps));
        }
    }

    private class OnCameraSettings implements ActivityResultDispatcher.Proc {

        public void run(Intent data) {
            mCameraParameters = new CameraParameters();
            String antibandingKey = CameraActivity.KEY_ANTIBANDING;
            String effectKey = CameraActivity.KEY_EFFECT;
            String flashModeKey = CameraActivity.KEY_FLASH_MODE;
            String focusModeKey = CameraActivity.KEY_FOCUS_MODE;
            String sceneModeKey = CameraActivity.KEY_SCENE_MODE;
            String wbKey = CameraActivity.KEY_WHITE_BALANCE;
            mCameraParameters.antibanding = data.getStringExtra(antibandingKey);
            mCameraParameters.effect = data.getStringExtra(effectKey);
            mCameraParameters.flashMode = data.getStringExtra(flashModeKey);
            mCameraParameters.focusMode = data.getStringExtra(focusModeKey);
            mCameraParameters.sceneMode = data.getStringExtra(sceneModeKey);
            mCameraParameters.whiteBalance = data.getStringExtra(wbKey);

            mCameraReader = new NopCameraReader();
        }
    }

    private class OnHostPreference implements ActivityResultDispatcher.Proc {

        public void run(Intent data) {
            String hostKey = HostPreferenceActivity.EXTRA_HOST;
            String host = data.getStringExtra(hostKey);
            int port = data.getIntExtra(HostPreferenceActivity.EXTRA_PORT, -1);
            writeHost(host, port);
        }
    }

    private class OnConfirmOk implements ActivityResultDispatcher.Proc {

        public void run(Intent data) {
            String path = getLogPath();
            try {
                mLogFile = new PrintWriter(path);
            }
            catch (FileNotFoundException e) {
                String msg = String.format("cannot open log: %s", path);
                ActivityUtil.showException(MainActivity.this, msg, e);
                return;
            }
            ActivityUtil.showToast(MainActivity.this, "executing ffmpeg...");
            mNexecClient.execute(data);
        }
    }

    private class Adapter extends BaseAdapter {

        private abstract class PositionalButtonListener
                implements OnClickListener {

            private int mPosition;

            public PositionalButtonListener(int position) {
                mPosition = position;
            }

            public void onClick(View view) {
                run(mPosition);
            }

            protected abstract void run(int position);
        }

        private class RemoveButtonListener extends PositionalButtonListener {

            public RemoveButtonListener(int position) {
                super(position);
            }

            protected void run(int position) {
                mFrames.remove(position);
                notifyDataSetChanged();
            }
        }

        private class MagnifyButtonListener extends PositionalButtonListener {

            public MagnifyButtonListener(int position) {
                super(position);
            }

            protected void run(int position) {
                String path = getOriginalFilePath(mFrames.get(position));
                showFile(path, "image/png");
            }
        }

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

            View removeButton = view.findViewById(R.id.remove_button);
            removeButton.setOnClickListener(new RemoveButtonListener(position));
            View magnifyButton = view.findViewById(R.id.magnify_button);
            OnClickListener l = new MagnifyButtonListener(position);
            magnifyButton.setOnClickListener(l);

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

        private DateFormat mDateFormat;

        public JpegCallback() {
            mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        }

        public void onPictureTaken(byte[] data, Camera camera) {
            String fileId = mDateFormat.format(new Date());

            String originalPath = getOriginalFilePath(fileId);
            try {
                saveFile(originalPath, data);

                String thumbnailPath = getThumbnailFilePath(fileId);
                saveThumbnail(originalPath, thumbnailPath);
            }
            catch (IOException e) {
                String msg = "failed to save";
                ActivityUtil.showException(MainActivity.this, msg, e);
                return;
            }

            addFrame(fileId);

            mCamera.startPreview();
        }

        private void saveThumbnail(String originalPath, String thumbnailPath) throws IOException {
            Bitmap bmp = BitmapFactory.decodeFile(originalPath);
            Bitmap thumb = Bitmap.createScaledBitmap(bmp, 320, 240, true);
            bmp.recycle();
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
            mShotButtonRunnable.run();
        }
    }

    private class SurfaceListener implements SurfaceHolder.Callback {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            }
            catch (IOException e) {
                String msg = "failed to show preview";
                ActivityUtil.showException(MainActivity.this, msg, e);
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCamera.stopPreview();
            Parameters params = mCamera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size bestSize = findBestPreviewSize(width, height, sizes);
            ViewUtil.resizeView(mView, bestSize);
            params.setPreviewSize(bestSize.width, bestSize.height);

            mCamera.setParameters(params);
            mCamera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
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

    private static class FrameRate {

        private int mRate;

        public FrameRate(int rate) {
            mRate = rate;
        }

        public String toString() {
            return Integer.toString(mRate);
        }

        public int toInteger() {
            return mRate;
        }
    }

    private static class CameraParameters {

        public String antibanding;
        public String effect;
        public String flashMode;
        public String focusMode;
        public String sceneMode;
        public String whiteBalance;

        public CameraParameters() {
        }

        public CameraParameters(Parameters params) {
            List<String> supportedAntibanding;
            supportedAntibanding = params.getSupportedAntibanding();
            if (supportedAntibanding != null) {
                antibanding = supportedAntibanding.get(0);
            }

            List<String> supportedColorEffects;
            supportedColorEffects = params.getSupportedColorEffects();
            if (supportedColorEffects != null) {
                effect = supportedColorEffects.get(0);
            }

            List<String> supportedFlashModes = params.getSupportedFlashModes();
            if (supportedFlashModes != null) {
                flashMode = supportedFlashModes.get(0);
            }

            List<String> supportedFocusModes = params.getSupportedFocusModes();
            if (supportedFocusModes != null) {
                focusMode = supportedFocusModes.get(0);
            }

            List<String> supportedSceneModes = params.getSupportedSceneModes();
            if (supportedSceneModes != null) {
                sceneMode = supportedSceneModes.get(0);
            }

            List<String> supportedWhiteBalance;
            supportedWhiteBalance = params.getSupportedWhiteBalance();
            if (supportedWhiteBalance != null) {
                whiteBalance = supportedWhiteBalance.get(0);
            }
        }

        public void updateTo(Parameters params) {
            CameraUtil.setAntibanding(params, antibanding);
            CameraUtil.setEffect(params, effect);
            CameraUtil.setFlashMode(params, flashMode);
            CameraUtil.setFocusMode(params, focusMode);
            CameraUtil.setSceneMode(params, sceneMode);
            CameraUtil.setWhiteBalance(params, whiteBalance);
        }
    }

    private interface FrameRateUpdater {

        public void run();
    }

    private class NopFrameRateUpdater implements FrameRateUpdater {

        public void run() {
        }
    }

    private class TrueFrameRateUpdater implements FrameRateUpdater {

        private FrameRate mValue;

        public TrueFrameRateUpdater(FrameRate value) {
            mValue = value;
        }

        public void run() {
            mFrameRate = mValue;
        }
    }

    private class ShotRunnable implements Runnable {

        public void run() {
            // People are saying that the internal buffer must be removed...
            // http://stackoverflow.com/questions/7627921/android-camera-takepicture-does-not-return-some-times
            // https://code.google.com/p/android/issues/detail?id=13966
            mCamera.setPreviewCallback(null);
            mCamera.takePicture(null, null, mJpegCallback);
        }
    }

    private class FocusAndShotRunnable implements Runnable {

        private class Callback implements Camera.AutoFocusCallback {

            public void onAutoFocus(boolean success, Camera camera) {
                mShotRunnable.run();
            }
        }

        private Camera.AutoFocusCallback mCallback = new Callback();

        public void run() {
            mCamera.autoFocus(mCallback);
        }
    }

    private class MenuActions {

        private SparseArray<MenuAction> mActions;
        private MenuAction mNopAction = new NopMenuAction();

        public MenuActions() {
            mActions = new SparseArray<MenuAction>();
        }

        public void put(int id, MenuAction action) {
            mActions.put(id, action);
        }

        public MenuAction get(int id) {
            MenuAction action = mActions.get(id);
            return action != null ? action : mNopAction;
        }
    }

    //private static final String TAG = "animator";

    private static final int REQUEST_CONFIRM = 0;
    private static final int REQUEST_HOST_PREFERENCE = 1;
    private static final int REQUEST_CAMERA_SETTINGS = 2;
    private static final int REQUEST_PROJECT_SETTINGS = 3;

    // Document
    private String mProjectDirectory;
    private List<String> mFrames = new ArrayList<String>();
    private FrameRate mFrameRate = new FrameRate(8);
    private String mHost = "neko-daisuki.ddo.jp";
    private int mPort = 57005;

    // View
    private SurfaceView mView;
    private Adapter mAdapter;

    // Helper
    private Camera mCamera;
    private CameraParameters mCameraParameters;
    private PictureCallback mJpegCallback = new JpegCallback();
    private NexecClient mNexecClient;
    private ActivityResultDispatcher mActivityResultDispatcher;
    private MenuActions mMenuActions;
    private PrintWriter mLogFile;
    private FrameRateUpdater mFrameRateUpdater = new NopFrameRateUpdater();
    private CameraReader mCameraReader = new DefaultCameraReader();
    private Runnable mShotButtonRunnable;
    private Runnable mShotRunnable = new ShotRunnable();
    private Runnable mFocusAndShotRunnable = new FocusAndShotRunnable();

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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_make_movie).setEnabled(0 < mFrames.size());
        disableMenu(menu, R.id.action_watch_log, getLogPath());
        disableMenu(menu, R.id.action_watch_movie, getMoviePath());

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeApplicationDirectory();

        View shotButton = findViewById(R.id.shot_button);
        shotButton.setOnClickListener(new ShotButtonOnClickListener());
        AdapterView<Adapter> list = (AdapterView<Adapter>)findViewById(R.id.list);
        mAdapter = new Adapter();
        list.setAdapter(mAdapter);

        View focusButton = findViewById(R.id.focus_button);
        focusButton.setOnClickListener(new FocusButtonListener());

        mView = (SurfaceView)findViewById(R.id.preview);
        mView.getHolder().addCallback(new SurfaceListener());
        int id = R.id.focus_area_view;
        FocusAreaView focusAreaView = (FocusAreaView)findViewById(id);
        focusAreaView.setSurfaceView(mView);
        focusAreaView.setOnAreaChangedListener(new FocusAreaListener());

        mNexecClient = new NexecClient(this);
        OnGetLineListener outListener = new OnGetLineListener();
        mNexecClient.setStdoutOnGetLineListener(outListener);
        mNexecClient.setStderrOnGetLineListener(outListener);
        mNexecClient.setOnFinishListener(new OnFinishListener());

        mActivityResultDispatcher = new ActivityResultDispatcher();
        mActivityResultDispatcher.put(
                REQUEST_CONFIRM, RESULT_OK, new OnConfirmOk());
        mActivityResultDispatcher.put(
                REQUEST_HOST_PREFERENCE, RESULT_OK, new OnHostPreference());
        mActivityResultDispatcher.put(
                REQUEST_CAMERA_SETTINGS, RESULT_OK, new OnCameraSettings());
        mActivityResultDispatcher.put(REQUEST_PROJECT_SETTINGS,
                                      RESULT_OK,
                                      new OnProjectSettings());

        mMenuActions = new MenuActions();
        mMenuActions.put(R.id.action_create_project, new CreateProjectAction());
        mMenuActions.put(R.id.action_rename_project, new RenameProjectAction());
        mMenuActions.put(R.id.action_clear_project, new ClearProjectAction());
        mMenuActions.put(R.id.action_open_project, new OpenProjectAction());
        mMenuActions.put(R.id.action_project_settings,
                         new ProjectSettingsAction());
        mMenuActions.put(R.id.action_host_settings, new HostSettingsAction());
        mMenuActions.put(R.id.action_make_movie, new MakeMovieAction());
        mMenuActions.put(R.id.action_camera_settings,
                         new CameraSettingsAction());
        mMenuActions.put(R.id.action_about, new AboutAction());
        mMenuActions.put(R.id.action_watch_log, new WatchLogAction());
        mMenuActions.put(R.id.action_watch_movie, new WatchMovieAction());
        mMenuActions.put(android.R.id.home, new NopMenuAction());
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultDispatcher.dispatch(requestCode, resultCode, data);
    }

    protected void onResume() {
        super.onResume();

        readHost();
        String defaultName = readDefaultProjectName();
        String projectName = defaultName != null ? defaultName : "default";
        changeProject(projectName);
        mFrameRateUpdater.run();

        mCamera = Camera.open();
        mCameraReader.run();
        updateCameraParameters();
    }

    protected void onPause() {
        super.onPause();

        mCamera.stopPreview();
        mCamera.release();

        writeCamera();
        writeProject();
        writeDefaultProjectName();
    }

    private String getThumbnailFilePath(String id) {
        return String.format("%s/%s-thumbnail.jpg", mProjectDirectory, id);
    }

    private String getOriginalFilePath(String id) {
        return String.format("%s/%s-original.jpg", mProjectDirectory, id);
    }

    private String getMoviePath() {
        return String.format("%s/movie.mp4", mProjectDirectory);
    }

    private void addLink(NexecClient.Settings settings, String id, int n) {
        String dest = getOriginalFilePath(id);
        Locale locale = Locale.US;
        String src = String.format(locale, "%s/%d.jpg", mProjectDirectory, n);
        settings.addLink(dest, src);
    }

    private void addLinks(NexecClient.Settings settings) {
        /*
         * Why does the following statement add the first image? Will the movie
         * have two frames of the first image?
         *
         * This is for a bug of ffmpeg[1][2]. ffmpeg makes an image2 movie with
         * a very short first image. This bug is not fixed still (2013-07-04).
         *
         * [1] https://ffmpeg.org/trac/ffmpeg/ticket/1578
         * [2] https://ffmpeg.org/trac/ffmpeg/ticket/1925
         *
         * Version of ffmpeg which I tried is 0.7.15.
         */
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
        files.add(getMoviePath());

        return files.toArray(new String[0]);
    }

    private String[] buildArgs() {
        return new String[] {
            "ffmpeg", "-loglevel", "quiet", "-y", "-r", mFrameRate.toString(),
            "-f", "image2", "-i",
            String.format("%s/%%d.jpg", mProjectDirectory), "-r", "24", "-s",
            "xga", getMoviePath() };
    }

    private String getLogPath() {
        return String.format("%s/ffmpeg.log", mProjectDirectory);
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
            String msg = String.format("failed to read %s", path);
            ActivityUtil.showException(this, msg, e);
        }
        return null;
    }

    private void initializeApplicationDirectory() {
        String dirpath = getApplicationDirectory();
        new File(dirpath).mkdirs();

        String nomedia = String.format("%s/.nomedia", dirpath);
        try {
            new File(nomedia).createNewFile();
        }
        catch (IOException e) {
            String msg = String.format("failed to create %s", nomedia);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private void changeProject(String name) {
        mProjectDirectory = getProjectDirectory(name);

        /*
         * When a user changes the project, the following statement is unneeded.
         * But it is harmless, too. So I placed it here to simplify the code.
         */
        new File(mProjectDirectory).mkdir();

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
                    else if (key.equals("frame_rate")) {
                        mFrameRate = new FrameRate(reader.nextInt());
                    }
                }
                reader.endObject();
            }
            finally {
                reader.close();
            }

            mAdapter.notifyDataSetChanged();
        }
        catch (IOException e) {
            String msg = String.format("failed to read %s", path);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private String getProjectJsonPath() {
        return String.format("%s/project.json", mProjectDirectory);
    }

    private JsonWriter makeJsonWriter(String path) throws IOException {
        JsonWriter writer = new JsonWriter(new FileWriter(path));
        writer.setIndent("    ");
        return writer;
    }

    private void writeProject() {
        String path = getProjectJsonPath();
        try {
            JsonWriter writer = makeJsonWriter(path);
            try {
                writer.beginObject();
                writer.name("frames");
                writer.beginArray();
                for (String id: mFrames) {
                    writer.value(id);
                }
                writer.endArray();
                writer.name("frame_rate").value(mFrameRate.toInteger());
                writer.endObject();
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            String msg = String.format("failed to write %s", path);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private String getHostJsonPath() {
        return String.format("%s/host.json", getApplicationDirectory());
    }

    private void writeHost(String host, int port) {
        String path = getHostJsonPath();
        try {
            JsonWriter writer = makeJsonWriter(path);
            try {
                writer.beginObject();
                writer.name("host").value(host);
                writer.name("port").value(port);
                writer.endObject();
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            String msg = String.format("failed to write %s", path);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private String getDefaultJsonPath() {
        return String.format("%s/default.json", getApplicationDirectory());
    }

    private void writeDefaultProjectName() {
        String path = getDefaultJsonPath();
        try {
            JsonWriter writer = makeJsonWriter(path);
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
            String msg = String.format("failed to write %s", path);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private String getProjectName() {
        return new File(mProjectDirectory).getName();
    }

    private void readHost() {
        String path = getHostJsonPath();
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
                    String name = reader.nextName();
                    if (name.equals("host")) {
                        mHost = reader.nextString();
                    }
                    else if (name.equals("port")) {
                        mPort = reader.nextInt();
                    }
                }
                reader.endObject();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            String msg = String.format("failed to read %s", path);
            ActivityUtil.showException(this, msg, e);
        }
    }

    private void clearFrames() {
        mFrames.clear();
        notifyDataSetChanged();
    }

    private void addFrame(String id) {
        mFrames.add(id);
        notifyDataSetChanged();
    }

    private void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private void disableMenu(Menu menu, int id, String path) {
        menu.findItem(id).setEnabled(new File(path).exists());
    }

    private String getCameraPath() {
        return String.format("%s/camera.json", getApplicationDirectory());
    }

    private void readCameraParameters() {
        String path = getCameraPath();
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
                    String name = reader.nextName();
                    String value = reader.nextString();
                    if (name.equals("antibanding")) {
                        mCameraParameters.antibanding = value;
                    }
                    else if (name.equals("effect")) {
                        mCameraParameters.effect = value;
                    }
                    else if (name.equals("flash_mode")) {
                        mCameraParameters.flashMode = value;
                    }
                    else if (name.equals("focus_mode")) {
                        mCameraParameters.focusMode = value;
                    }
                    else if (name.equals("scene_mode")) {
                        mCameraParameters.sceneMode = value;
                    }
                    else if (name.equals("white_balance")) {
                        mCameraParameters.whiteBalance = value;
                    }
                }
                reader.endObject();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            String fmt = "failed to read camera settings: %s";
            ActivityUtil.showException(this, String.format(fmt, path), e);
        }
    }

    private void writeJsonValue(JsonWriter writer, String name, String value)
    throws IOException {
        if (value == null) {
            return;
        }
        writer.name(name);
        writer.value(value);
    }

    private void writeCamera() {
        String path = getCameraPath();
        try {
            JsonWriter writer = makeJsonWriter(path);
            try {
                writer.beginObject();
                writeJsonValue(writer,
                               "antibanding",
                               mCameraParameters.antibanding);
                writeJsonValue(writer, "effect", mCameraParameters.effect);
                writeJsonValue(writer,
                               "flash_mode",
                               mCameraParameters.flashMode);
                writeJsonValue(writer,
                               "focus_mode",
                               mCameraParameters.focusMode);
                writeJsonValue(writer,
                               "scene_mode",
                               mCameraParameters.sceneMode);
                writeJsonValue(writer,
                               "white_balance",
                               mCameraParameters.whiteBalance);
                writer.endObject();
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            String fmt = "failed to write camera settings: %s";
            ActivityUtil.showException(this, String.format(fmt, path), e);
        }
    }

    private void updateCameraParameters() {
        Parameters params = mCamera.getParameters();
        mCameraParameters.updateTo(params);
        mCamera.setParameters(params);
        String focusMode = mCameraParameters.focusMode;
        mShotButtonRunnable = focusMode.equals(Parameters.FOCUS_MODE_AUTO)
                ? mFocusAndShotRunnable : mShotRunnable;
    }

    private void showFile(String path, String type) {
        Uri uri = Uri.parse(String.format("file://%s", path));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, type);
        startActivity(intent);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
