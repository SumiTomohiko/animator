package jp.gr.java_conf.neko_daisuki.android.animator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class LogActivity extends Activity {

    private enum Key {
        LOG_PATH
    }

    public static final String KEY_LOG_PATH = Key.LOG_PATH.name();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        StringBuilder buffer = new StringBuilder();
        String path = getIntent().getStringExtra(KEY_LOG_PATH);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
        }
        catch (FileNotFoundException e) {
            showException(e);
            return;
        }
        try {
            String l;
            while ((l = reader.readLine()) != null) {
                buffer.append(l + "\n");
            }
        }
        catch (IOException e) {
            showException(e);
        }

        TextView text = (TextView)findViewById(R.id.log_text);
        text.setText(buffer.toString());
    }

    private void showException(Exception e) {
        e.printStackTrace();
        showToast(e.getMessage());
    }

    private void showToast(String msg) {
        String fmt = "animator: %s";
        Toast.makeText(this, String.format(fmt, msg), Toast.LENGTH_LONG).show();
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
