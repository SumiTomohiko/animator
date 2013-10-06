package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.IOException;
import java.util.List;

import android.content.Intent;
import android.hardware.Camera.Parameters;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Context;

public class CameraActivity extends FragmentActivity {

    private abstract class ParamButtonListener implements OnClickListener {

        private class ListListener implements OnItemClickListener {

            private String[] mItems;

            public ListListener(String[] items) {
                mItems = items;
            }

            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {
                Parameters params = mCamera.getParameters();
                updateParameter(params, mItems[position]);
                mCamera.setParameters(params);
            }
        }

        public void onClick(View view) {
            Context ctx = CameraActivity.this;
            Parameters params = mCamera.getParameters();
            String[] items = listItems(params).toArray(new String[0]);
            int resource = android.R.layout.simple_list_item_1;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx,
                                                                    resource,
                                                                    items);
            mParamsList.setAdapter(adapter);
            mParamsList.setOnItemClickListener(new ListListener(items));
        }

        protected abstract List<String> listItems(Parameters params);
        protected abstract void updateParameter(Parameters params,
                                                String value);
    }

    private class AntibandingButtonListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedAntibanding();
        }

        protected void updateParameter(Parameters params, String value) {
            mAntibanding = value;
            params.setAntibanding(value);
            mAntibandingText.setText(value);
        }
    }

    private class EffectButtonListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedColorEffects();
        }

        protected void updateParameter(Parameters params, String value) {
            mEffect = value;
            params.setColorEffect(value);
            mEffectText.setText(value);
        }
    }

    private class FlashModeListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedFlashModes();
        }

        protected void updateParameter(Parameters params, String value) {
            mFlashMode = value;
            params.setFlashMode(value);
            mFlashModeText.setText(value);
        }
    }

    private class FocusModeListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedFocusModes();
        }

        protected void updateParameter(Parameters params, String value) {
            mFocusMode = value;
            params.setFocusMode(value);
            mFocusModeText.setText(value);
        }
    }

    private class SceneModeListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedSceneModes();
        }

        protected void updateParameter(Parameters params, String value) {
            mSceneMode = value;
            params.setSceneMode(value);
            mSceneModeText.setText(value);
        }
    }

    private class WhiteBalanceListener extends ParamButtonListener {

        protected List<String> listItems(Parameters params) {
            return params.getSupportedWhiteBalance();
        }

        protected void updateParameter(Parameters params, String value) {
            mWhiteBalance = value;
            params.setWhiteBalance(value);
            mWhiteBalanceText.setText(value);
        }
    }

    private abstract class CloseButtonListener implements OnClickListener {

        public void onClick(View view) {
            Intent intent = getIntent();
            setResult(run(intent), intent);
            finish();
        }

        protected abstract int run(Intent intent);
    }

    private class OkayButtonListener extends CloseButtonListener {

        protected int run(Intent intent) {
            intent.putExtra(KEY_ANTIBANDING, mAntibanding);
            intent.putExtra(KEY_EFFECT, mEffect);
            intent.putExtra(KEY_FLASH_MODE, mFlashMode);
            intent.putExtra(KEY_FOCUS_MODE, mFocusMode);
            intent.putExtra(KEY_SCENE_MODE, mSceneMode);
            intent.putExtra(KEY_WHITE_BALANCE, mWhiteBalance);
            return RESULT_OK;
        }
    }

    private class CancelButtonListener extends CloseButtonListener {

        protected int run(Intent intent) {
            return RESULT_CANCELED;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            }
            catch (IOException e) {
                String msg = "cannot show camera preview";
                ActivityUtil.showException(CameraActivity.this, msg, e);
                return;
            }

            Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size minimum = selectMinimumSize(sizes);
            ViewUtil.resizeView(mSurfaceView, minimum);
            params.setPreviewSize(minimum.width, minimum.height);
            mCamera.setParameters(params);

            mCamera.startPreview();
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format,
                                   int width,
                                   int height) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        private Camera.Size selectMinimumSize(List<Camera.Size> sizes) {
            Camera.Size minimum = sizes.get(0);
            int len = sizes.size();
            for (int i = 1; i < len; i++) {
                Camera.Size size = sizes.get(i);
                minimum = size.width < minimum.width ? size : minimum;
            }
            return minimum;
        }
    }

    public static final String KEY_ANTIBANDING = "antibanding";
    public static final String KEY_EFFECT = "effect";
    public static final String KEY_FLASH_MODE = "flashMode";
    public static final String KEY_FOCUS_MODE = "focusMode";
    public static final String KEY_SCENE_MODE = "sceneMode";
    public static final String KEY_WHITE_BALANCE = "whiteBalance";

    // document
    private String mAntibanding;
    private String mEffect;
    private String mFlashMode;
    private String mFocusMode;
    private String mSceneMode;
    private String mWhiteBalance;

    // view
    private SurfaceView mSurfaceView;
    private TextView mAntibandingText;
    private TextView mEffectText;
    private TextView mFlashModeText;
    private TextView mFocusModeText;
    private TextView mSceneModeText;
    private TextView mWhiteBalanceText;
    private AdapterView<ArrayAdapter<String>> mParamsList;

    // helpers
    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // document
        Intent i = getIntent();
        mAntibanding = i.getStringExtra(KEY_ANTIBANDING);
        mEffect = i.getStringExtra(KEY_EFFECT);
        mFlashMode = i.getStringExtra(KEY_FLASH_MODE);
        mFocusMode = i.getStringExtra(KEY_FOCUS_MODE);
        mSceneMode = i.getStringExtra(KEY_SCENE_MODE);
        mWhiteBalance = i.getStringExtra(KEY_WHITE_BALANCE);

        // view
        setContentView(R.layout.activity_camera);

        mSurfaceView = (SurfaceView)findViewById(R.id.preview);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());

        mAntibandingText = findTextView(R.id.antibanding_text);
        mEffectText = findTextView(R.id.effect_text);
        mFlashModeText = findTextView(R.id.flash_mode_text);
        mFocusModeText = findTextView(R.id.focus_mode_text);
        mSceneModeText = findTextView(R.id.scene_mode_text);
        mWhiteBalanceText = findTextView(R.id.white_balance_text);

        int id = R.id.params_list;
        mParamsList = (AdapterView<ArrayAdapter<String>>)findViewById(id);

        View okayButton = findViewById(R.id.positive_button);
        okayButton.setOnClickListener(new OkayButtonListener());
        View cancelButton = findViewById(R.id.negative_button);
        cancelButton.setOnClickListener(new CancelButtonListener());
    }

    protected void onResume() {
        super.onResume();

        mCamera = Camera.open();
        Parameters params = mCamera.getParameters();
        CameraUtil.setAntibanding(params, mAntibanding);
        CameraUtil.setEffect(params, mEffect);
        CameraUtil.setFlashMode(params, mFlashMode);
        CameraUtil.setFocusMode(params, mFocusMode);
        CameraUtil.setSceneMode(params, mSceneMode);
        CameraUtil.setWhiteBalance(params, mWhiteBalance);
        mCamera.setParameters(params);

        boolean antibandingSupported = params.getSupportedAntibanding() != null;
        boolean effectSupported = params.getSupportedColorEffects() != null;
        boolean flashModeSupported = params.getSupportedFlashModes() != null;
        boolean focusModeSupported = params.getSupportedFocusModes() != null;
        boolean sceneModeSupported = params.getSupportedSceneModes() != null;
        List<String> supportedWhiteBalance = params.getSupportedWhiteBalance();
        boolean whiteBalanceSupported = supportedWhiteBalance != null;

        String notSupported = "Not supported";
        String s = antibandingSupported ? mAntibanding : notSupported;
        mAntibandingText.setText(s);
        mEffectText.setText(effectSupported ? mEffect : notSupported);
        mFlashModeText.setText(flashModeSupported ? mFlashMode : notSupported);
        mFocusModeText.setText(focusModeSupported ? mFocusMode : notSupported);
        mSceneModeText.setText(sceneModeSupported ? mSceneMode : notSupported);
        String t = whiteBalanceSupported ? mWhiteBalance : notSupported;
        mWhiteBalanceText.setText(t);

        View antibandingButton = findViewById(R.id.antibanding_button);
        View effectButton = findViewById(R.id.effect_button);
        View flashModeButton = findViewById(R.id.flash_mode_button);
        View focusModeButton = findViewById(R.id.focus_mode_button);
        View sceneModeButton = findViewById(R.id.scene_mode_button);
        View whiteBalanceButton = findViewById(R.id.white_balance_button);

        antibandingButton.setOnClickListener(
                antibandingSupported ? new AntibandingButtonListener() : null);
        antibandingButton.setEnabled(antibandingSupported);
        effectButton.setOnClickListener(
                effectSupported ? new EffectButtonListener() : null);
        effectButton.setEnabled(effectSupported);
        flashModeButton.setOnClickListener(
                flashModeSupported ? new FlashModeListener() : null);
        flashModeButton.setEnabled(flashModeSupported);
        focusModeButton.setOnClickListener(
                focusModeSupported ? new FocusModeListener() : null);
        focusModeButton.setEnabled(focusModeSupported);
        sceneModeButton.setOnClickListener(
                sceneModeSupported ? new SceneModeListener() : null);
        sceneModeButton.setEnabled(sceneModeSupported);
        whiteBalanceButton.setOnClickListener(
                whiteBalanceSupported ? new WhiteBalanceListener() : null);
        whiteBalanceButton.setEnabled(whiteBalanceSupported);
    }

    protected void onPause() {
        super.onPause();
        mCamera.release();
    }

    private TextView findTextView(int id) {
        return (TextView)findViewById(id);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
