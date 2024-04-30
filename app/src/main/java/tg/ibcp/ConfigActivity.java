package tg.ibcp;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;



public class ConfigActivity extends AppCompatActivity {
    public LinkProperties mlp;
    public static final int NOTIF_ID_WRONGPASS = 32423;
    public static final int NOTIF_ID_SETCREDS = 42754;
    public static final String SANITIZED_AMPERSAND = "ibcp.AMPERSAND"; // external
    public boolean firstRun = false;
    private ConnectivityManager connManager;
    public CaptivePortal cp;
    private ConnectivityManager getConnManager(){
        if(connManager==null){
            connManager = (ConnectivityManager) ConfigActivity.this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return connManager;
    }

    private class WebViewHelper {
        private WebView wb;
        protected String notifAccess;
        protected String username;
        protected String loginTime;
        protected SharedPreferences prefs;

        private SharedPreferences getPrefs() {
            if (prefs == null)
                prefs = getApplication().getSharedPreferences(getString(R.string.defeaultPrefs), Context.MODE_PRIVATE);
            return prefs;
        }

        private String getLoginTime() {
            if (loginTime == null) {
                loginTime = String.valueOf(getPrefs().getLong(getString(R.string.prefs_login_time), 0));
            }
            return loginTime;
        }

        private String getUsername() {
            if (username == null) {
                username = getPrefs().getString(getString(R.string.prefs_user), "");
            }
            return username;
        }

        private String getNotifAccess() {
            if (notifAccess == null) {
                notifAccess = String.valueOf(NotificationManagerCompat.getEnabledListenerPackages(ConfigActivity.this).contains(getPackageName()));
            }
            return notifAccess;
        }

        private void setNotifAccess(boolean b) {
            notifAccess = String.valueOf(b);
        }

        private void setUsername(String str) {
            username = str;
        }

        private void setLoginTime(String str) {
            loginTime = str;
        }

        private WebViewHelper() {
            wb = ConfigActivity.this.findViewById(R.id.mainwb);
            wb.setBackgroundColor(Color.TRANSPARENT);
            wb.getSettings().setJavaScriptEnabled(true);
            wb.getSettings().setAllowFileAccessFromFileURLs(true);
//            wb.setOnTouchListener((v, event) -> (event.getAction() == MotionEvent.ACTION_MOVE));
            wb.addJavascriptInterface(new JsInterface(ConfigActivity.this, this), "IBCP" /* external */);
        }

        private void drawWebUi() {
            if (wb == null) {
                wb = ConfigActivity.this.findViewById(R.id.mainwb);
            }
            if (wb.getUrl() == null) {
                wb.loadUrl("file:///android_res/raw/index.html/?"
                        + getNotifAccess() + "&"
                        + getUsername().replaceAll("&", SANITIZED_AMPERSAND) + "&"
                        + getLoginTime()
                );
            } else {
                wb.evaluateJavascript("pageSetup('" + getNotifAccess() + "','" + getUsername().replaceAll("&", SANITIZED_AMPERSAND) + "','" + getLoginTime() + "')", null);
            }
        }

        class JsInterface {
            Context ctx;
            WebViewHelper wvh;

            JsInterface(Context ctx, WebViewHelper wvh) {
                this.ctx = ctx;
                this.wvh = wvh;
            }

            @JavascriptInterface
            public void detailUpdate(String id, String pass) {
                if (id.isEmpty() || pass.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(ctx, "Username/Password cannot be empty!", Toast.LENGTH_SHORT).show();
                            }
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
                        }
                );
                SignInUtils.signInIfRequired(mlp, ctx);
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
            if (mWebViewHelper != null) {
                long loginTime = intent.getLongExtra(getString(R.string.login_time_extra), -0x69);
                if (loginTime != -0x69) {
                    try{
                        cp.reportCaptivePortalDismissed();
                    }catch (Exception ignored){}
                    if(loginTime!=-1 && isCaptivePortalSignInAction() && !firstRun){
                        ConfigActivity.this.finishAndRemoveTask();
                    }else {
                        mWebViewHelper.setLoginTime(String.valueOf(loginTime));
                        mWebViewHelper.drawWebUi();
                    }
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        findViewById(R.id.aboutButton).setOnClickListener(v -> {
            showAboutDialog();
        });
        mWebViewHelper = new WebViewHelper();
        mWebViewHelper.drawWebUi();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(getString(R.string.configactivity_broadcast_receiver)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // no time to clean this up cuz exams, looks super ugly and is inefficient
        if(isCaptivePortalSignInAction()) {
            mlp = handleIfAlienNetwork();
        }else{
            //bind to wifi network (and not cellular etc):
            mlp = NetworkUtils.bindProcessToWiFiNetwork(getConnManager());
        }
        if (mWebViewHelper == null) {
            mWebViewHelper = new WebViewHelper();
        }
        if (isFirstRun()) {
            firstRun=true;
            showAboutDialog();
        } else {
            getNotificationPermission();
        }
        if(!"".equals(mWebViewHelper.getUsername())){
            SignInUtils.signInIfRequired(mlp, ConfigActivity.this);
        }
        boolean notifAccess = NotificationManagerCompat.getEnabledListenerPackages(ConfigActivity.this).contains(getPackageName());
        if (!mWebViewHelper.getNotifAccess().equals(String.valueOf(notifAccess))) {
            mWebViewHelper.setNotifAccess(notifAccess);
            mWebViewHelper.drawWebUi();
        }
        cancelNotifications();
    }

    private boolean isFirstRun() {
        final String PREF_KEY = "firstrun";
        SharedPreferences prefs = getSharedPreferences("bogusStr", MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY, true)) {
            prefs.edit().putBoolean(PREF_KEY, false).apply();
            return true;
        }
        return false;
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ConfigActivity.this, R.style.AlertDialogCustom));
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle("About");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.about_dialog, null);
        alertDialog.setView(dialogView);
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setDimAmount(0.9f);
        }
        TextView tv = dialogView.findViewById(R.id.clickable_tv1);
        tv.setText(HtmlCompat.fromHtml("This project is <a href='https://github.com/savsch/serenity'>open source</a>.\n<br><b>Author:</b> <a href='https://github.com/savsch'>savsch</a>", HtmlCompat.FROM_HTML_MODE_COMPACT));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (DialogInterface dialog, int which) -> {
            ConfigActivity.this.getNotificationPermission();
            dialog.dismiss();
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "SHARE", (DialogInterface dialog, int which) -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Tired of logging into institute wifi on your phone again and again? Check this out: https://play.google.com/store/apps/details?id=" + ConfigActivity.this.getPackageName());
            sendIntent.setType("text/plain");
            ConfigActivity.this.startActivity(sendIntent);
            dialog.dismiss();
        });
        alertDialog.show();
    }

    public void getNotificationPermission() {
        try {
            //TODO: add permission request rationale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        0xBAD/*UNUSED*/);
            }
        } catch (Exception ignored) {
        }
    }

    public void cancelNotifications() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIF_ID_SETCREDS);
        notificationManager.cancel(NOTIF_ID_WRONGPASS);
    }
    private boolean isCaptivePortalSignInAction(){
        return CompatUtils.isCaptivePortalIntent(ConfigActivity.this.getIntent().getAction());
    }
    private LinkProperties handleIfAlienNetwork(){
        boolean bindSuccess;
        Network captiveNetwork = getIntent().getParcelableExtra(ConnectivityManager.EXTRA_NETWORK);
        cp = getIntent().getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL);
        LinkProperties lp = null;
        if (captiveNetwork != null) {
            bindSuccess = getConnManager().bindProcessToNetwork(captiveNetwork);
            lp = getConnManager().getLinkProperties(captiveNetwork);
        } else {
            bindSuccess = false;
        }
        SignInUtils.isIntendedWiFiNetwork(lp, value -> {
            if (!value || !bindSuccess) {
                Intent i = new Intent();
                i.setAction(getIntent().getAction());
                Bundle b = getIntent().getExtras();
                if(b!=null) {
                    i.putExtras(b);
                }
                i.setPackage(CompatUtils.getDefaultCaptivePortalPackage());
                startActivity(i);
            }
        });
        return lp;
    }
}