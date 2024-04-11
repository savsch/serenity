package tg.ibcp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;


public class ConfigActivity extends AppCompatActivity {
    public static final int NOTIF_ID_WRONGPASS = 32423;
    public static final int NOTIF_ID_SETCREDS = 42754;
    public static final String SANITIZED_AMPERSAND = "ibcp.AMPERSAND"; // external
    private class WebViewHelper {
        private WebView wb;
        protected String notifAccess;
        protected String username;
        protected String loginTime;
        protected SharedPreferences prefs;
        private SharedPreferences getPrefs(){
            if(prefs==null)
                prefs = getApplication().getSharedPreferences(getString(R.string.defeaultPrefs), Context.MODE_PRIVATE);
            return prefs;
        }
        private String getLoginTime(){
            if(loginTime==null){
                loginTime = String.valueOf(getPrefs().getLong(getString(R.string.prefs_login_time),0));
            }
            return loginTime;
        }
        private String getUsername(){
            if(username==null){
                username = getPrefs().getString(getString(R.string.prefs_user),"");
            }
            return username;
        }
        private String getNotifAccess(){
            if(notifAccess==null){
                notifAccess = String.valueOf(NotificationManagerCompat.getEnabledListenerPackages(ConfigActivity.this).contains(getPackageName()));
            }
            return notifAccess;
        }
        private void setNotifAccess(boolean b){
            notifAccess=String.valueOf(b);
        }
        private void setUsername(String str){
            username=str;
        }
        private void setLoginTime(String str){
            loginTime=str;
        }

        private WebViewHelper(){
            wb=ConfigActivity.this.findViewById(R.id.mainwb);
            wb.setBackgroundColor(Color.TRANSPARENT);
            wb.getSettings().setJavaScriptEnabled(true);
            wb.getSettings().setAllowFileAccessFromFileURLs(true);
            wb.setOnTouchListener((v, event) -> (event.getAction() == MotionEvent.ACTION_MOVE));
            wb.addJavascriptInterface(new JsInterface(ConfigActivity.this, this) , "IBCP" /* external */ );
        }
        private void drawWebUi(){
            if(wb==null) {
                wb=ConfigActivity.this.findViewById(R.id.mainwb);
            }
            if(wb.getUrl()==null) {
                wb.loadUrl("file:///android_res/raw/index.html/?"
                        + getNotifAccess() + "&"
                        + getUsername().replaceAll("&",SANITIZED_AMPERSAND) + "&"
                        + getLoginTime()
                );
            }else{
                wb.evaluateJavascript("pageSetup('"+getNotifAccess()+"','"+getUsername().replaceAll("&",SANITIZED_AMPERSAND)+"','"+getLoginTime()+"')", null);
            }
        }
        class JsInterface {
            Context ctx;
            WebViewHelper wvh;
            JsInterface(Context ctx, WebViewHelper wvh){
                this.ctx=ctx;
                this.wvh=wvh;
            }
            @JavascriptInterface
            public void detailUpdate(String id, String pass) {
                if(id.isEmpty() || pass.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ctx, "Username/Password cannot be empty!", Toast.LENGTH_SHORT).show();}
                    );
                    return;
                }
                SharedPreferences.Editor speditor = ctx.getSharedPreferences(ctx.getString(R.string.defeaultPrefs), Context.MODE_PRIVATE).edit();
                speditor.putString(ctx.getString(R.string.prefs_user), id);
                speditor.putString(ctx.getString(R.string.prefs_pass), pass);
                speditor.putLong(ctx.getString(R.string.prefs_login_time), 0);
                speditor.apply();
                new Handler(Looper.getMainLooper()).post(() -> {
                            wvh.setUsername(id);
                            wvh.setLoginTime("0");
                            wvh.drawWebUi();
                            Toast.makeText(ctx, "Automatic Login Credentials Set", Toast.LENGTH_SHORT).show();
                        }
                );
                SignInUtils.signInIfRequired(ctx);
            }

            @JavascriptInterface
            public void notifRequest() {
                ctx.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        }
    }
    private WebViewHelper mWebViewHelper;
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mWebViewHelper!=null){
                long loginTime = intent.getLongExtra(getString(R.string.login_time_extra), -0x69);
                if(loginTime!=-0x69) {
                    mWebViewHelper.setLoginTime(String.valueOf(loginTime));
                    mWebViewHelper.drawWebUi();
                }
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        mWebViewHelper = new WebViewHelper();
        mWebViewHelper.drawWebUi();
        getNotificationPermission();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(getString(R.string.configactivity_broadcast_receiver)));
        cancelNotifications();
    }

    @Override
    protected void onResume() {
        if(mWebViewHelper==null){
            mWebViewHelper = new WebViewHelper();
        }
        boolean notifAccess = NotificationManagerCompat.getEnabledListenerPackages(ConfigActivity.this).contains(getPackageName());
        if(!mWebViewHelper.getNotifAccess().equals(String.valueOf(notifAccess))){
            mWebViewHelper.setNotifAccess(notifAccess);
            mWebViewHelper.drawWebUi();
        }
        cancelNotifications();
        super.onResume();
    }

    public void getNotificationPermission(){
        try {
            //TODO: add permission request rationale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        0xBAD/*UNUSED*/);
            }
        }catch (Exception ignored){}
    }
    public void cancelNotifications(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIF_ID_SETCREDS);
        notificationManager.cancel(NOTIF_ID_WRONGPASS);
    }
}