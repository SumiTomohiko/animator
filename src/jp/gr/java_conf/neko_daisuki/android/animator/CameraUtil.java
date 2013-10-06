package jp.gr.java_conf.neko_daisuki.android.animator;

import java.util.List;

import android.hardware.Camera.Parameters;

public class CameraUtil {

    public static void setAntibanding(Parameters params, String value) {
        List<String> supported = params.getSupportedAntibanding();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setAntibanding(supportedValue);
    }

    public static void setEffect(Parameters params, String value) {
        List<String> supported = params.getSupportedColorEffects();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setColorEffect(supportedValue);
    }

    public static void setFlashMode(Parameters params, String value) {
        List<String> supported = params.getSupportedFlashModes();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setFlashMode(supportedValue);
    }

    public static void setFocusMode(Parameters params, String value) {
        List<String> supported = params.getSupportedFocusModes();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setFocusMode(supportedValue);
    }

    public static void setSceneMode(Parameters params, String value) {
        List<String> supported = params.getSupportedSceneModes();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setSceneMode(supportedValue);
    }

    public static void setWhiteBalance(Parameters params, String value) {
        List<String> supported = params.getSupportedWhiteBalance();
        String supportedValue = getSupportedValue(supported, value);
        if (supportedValue == null) {
            return;
        }
        params.setWhiteBalance(supportedValue);
    }

    private static String getSupportedValue(List<String> supported,
                                            String value) {
        return (supported != null) && supported.contains(value) ? value : null;
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
