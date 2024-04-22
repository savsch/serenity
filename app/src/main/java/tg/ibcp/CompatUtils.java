package tg.ibcp;

import android.net.ConnectivityManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CompatUtils {
    public static final String MIUI_CAPTIVE_PORTAL_ACTION = "com.miui.action.OPEN_WIFI_LOGIN";
    public static final String MIUI_CAPTIVE_PORTAL_PACKAGE = "com.android.htmlviewer";
    public static boolean isCaptivePortalIntent(String action){
        return ConnectivityManager.ACTION_CAPTIVE_PORTAL_SIGN_IN.equals(action) || MIUI_CAPTIVE_PORTAL_ACTION.equals(action);
    }
    public static String getDefaultCaptivePortalPackage(){
        String defpak = "com.google.android.captiveportallogin"; //TODO: use queryIntentActivities to avoid hardcoding, but will require <queries> in manifest and i am lazy
        try{
            if(isMiUi()){
                defpak = MIUI_CAPTIVE_PORTAL_PACKAGE;
            }
        }catch (Exception ignored){}
        return defpak;
    }
    public static boolean isMiUi() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }
}
