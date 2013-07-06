package jp.gr.java_conf.neko_daisuki.android.animator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.EditText;

public class HostPreferenceActivity extends Activity {

    private class PositiveButtonOnClickListener implements OnClickListener {

        public void onClick(View view) {
            String host = mHostEdit.getText().toString();
            if (host.equals("")) {
                return;
            }

            int port;
            try {
                port = Integer.parseInt(mPortEdit.getText().toString());
            }
            catch (NumberFormatException e) {
                return;
            }
            if ((port < 1) || (65535 < port)) {
                return;
            }

            Intent intent = getIntent();
            intent.putExtra(EXTRA_HOST, host);
            intent.putExtra(EXTRA_PORT, port);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private class NegativeButtonOnClickListener implements OnClickListener {

        public void onClick(View view) {
            setResult(RESULT_CANCELED, getIntent());
            finish();
        }
    }

    private enum Extra {
        Host,
        Port;
    }

    public static final String EXTRA_HOST = Extra.Host.name();
    public static final String EXTRA_PORT = Extra.Port.name();

    private EditText mHostEdit;
    private EditText mPortEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_preference);

        mHostEdit = (EditText)findViewById(R.id.host);
        mPortEdit = (EditText)findViewById(R.id.port);
        Intent intent = getIntent();
        mHostEdit.setText(intent.getStringExtra(EXTRA_HOST));
        mPortEdit.setText(Integer.toString(intent.getIntExtra(EXTRA_PORT, -1)));

        View positiveButton = findViewById(R.id.positive);
        positiveButton.setOnClickListener(new PositiveButtonOnClickListener());
        View negativeButton = findViewById(R.id.negative);
        negativeButton.setOnClickListener(new NegativeButtonOnClickListener());
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
