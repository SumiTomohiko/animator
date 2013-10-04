package jp.gr.java_conf.neko_daisuki.android.animator;

import android.hardware.Camera;
import android.view.View;
import android.view.ViewGroup;

public class ViewUtil {

    public static void resizeView(View view, Camera.Size size) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = size.width;
        params.height = size.height;
        view.setLayoutParams(params);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
