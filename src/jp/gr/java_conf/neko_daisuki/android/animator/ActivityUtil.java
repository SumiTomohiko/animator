package jp.gr.java_conf.neko_daisuki.android.animator;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class ActivityUtil {

    private static final String TAG = "ActivityUtil";

    public static void showException(Activity activity,
                                     String msg,
                                     Throwable e) {
        e.printStackTrace();

        String s = String.format("%s: %s", msg, e.getMessage());
        showToast(activity, s);
        Log.e(TAG, s);
    }

    public static void showToast(Activity activity, String msg) {
        String s = String.format("animator: %s", msg);
        Toast.makeText(activity, s, Toast.LENGTH_LONG).show();
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
