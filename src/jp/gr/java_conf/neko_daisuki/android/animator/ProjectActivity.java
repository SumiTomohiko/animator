package jp.gr.java_conf.neko_daisuki.android.animator;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.EditText;

public class ProjectActivity extends Activity {

    private class OkayButtonListener implements OnClickListener {

        public void onClick(View view) {
            String s = mFpsEdit.getText().toString();
            int fps;
            try {
                fps = Integer.parseInt(s);
            }
            catch (NumberFormatException e) {
                // TODO: Show with a dialog.
                String msg = "malformed frame rate";
                ActivityUtil.showToast(ProjectActivity.this, msg);
                return;
            }
            if ((fps < 1) || (24 < fps)) {
                // TODO: Show with a dialog.
                String msg = "Frame rate must be between 1 and 24.";
                ActivityUtil.showToast(ProjectActivity.this, msg);
                return;
            }

            Intent intent = getIntent();
            intent.putExtra(KEY_FPS, fps);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private class CancelButtonListener implements OnClickListener {

        public void onClick(View view) {
            setResult(RESULT_CANCELED, getIntent());
            finish();
        }
    }

    public static final String KEY_FPS = "fps";

    private EditText mFpsEdit;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int fps = intent.getIntExtra(KEY_FPS, 8);

        setContentView(R.layout.activity_project);
        mFpsEdit = (EditText)findViewById(R.id.fps_text);
        mFpsEdit.setText(Integer.toString(fps));
        View okayButton = findViewById(R.id.positive_button);
        okayButton.setOnClickListener(new OkayButtonListener());
        View cancelButton = findViewById(R.id.negative_button);
        cancelButton.setOnClickListener(new CancelButtonListener());
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
