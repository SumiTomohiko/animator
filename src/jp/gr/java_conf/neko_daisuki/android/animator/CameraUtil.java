package jp.gr.java_conf.neko_daisuki.android.animator;

import java.util.List;

import android.hardware.Camera.Parameters;

public class CameraUtil {

    public static void setAntibanding(Parameters params, String value) {
        List<String> supported = params.getSupportedAntibanding();
        params.setAntibanding(getSupportedValue(supported, value));
    }

    public static void setEffect(Parameters params, String value) {
        List<String> supported = params.getSupportedColorEffects();
        params.setColorEffect(getSupportedValue(supported, value));
    }

    public static void setFlashMode(Parameters params, String value) {
        List<String> supported = params.getSupportedFlashModes();
        params.setFlashMode(getSupportedValue(supported, value));
    }

    public static void setFocusMode(Parameters params, String value) {
        List<String> supported = params.getSupportedFocusModes();
        params.setFocusMode(getSupportedValue(supported, value));
    }

    public static void setSceneMode(Parameters params, String value) {
        List<String> supported = params.getSupportedSceneModes();
        params.setSceneMode(getSupportedValue(supported, value));
    }

    public static void setWhiteBalance(Parameters params, String value) {
        List<String> supported = params.getSupportedWhiteBalance();
        params.setWhiteBalance(getSupportedValue(supported, value));
    }

    private static String getSupportedValue(List<String> supported,
                                            String value) {
        return supported.contains(value) ? value : null;
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
