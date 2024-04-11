package tg.ibcp;

import static tg.ibcp.NotificationUtils.notifyCredentialsNotSet;
import static tg.ibcp.NotificationUtils.notifyWrongPass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringEscapeUtils;


public class NotificationListener extends NotificationListenerService {
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        if(
                sbn.getPackageName().equals("android")
                        && sbn.getNotification().extras.getString("android.text")!=null
                        && sbn.getNotification().extras.getString("android.text").contains("IIT(BHU)")
        ){
            SharedPreferences sp = getApplication().getSharedPreferences(getString(R.string.defeaultPrefs), Context.MODE_PRIVATE);
            final String user = StringEscapeUtils.escapeEcmaScript(sp.getString(getString(R.string.prefs_user),""));
            final String pass = StringEscapeUtils.escapeEcmaScript(sp.getString(getString(R.string.prefs_pass),""));
            final long loginTime = sp.getLong(getString(R.string.prefs_login_time),0);
            if(pass.length()==0){
                notifyCredentialsNotSet(NotificationListener.this);
            }else if(loginTime==-1){
                notifyWrongPass(NotificationListener.this);
            }else{
                WebView wb = new WebView(NotificationListener.this);
                wb.getSettings().setJavaScriptEnabled(true);
                wb.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if (url.contains("keepalive")) {
                            try {
                                Toast.makeText(NotificationListener.this, "Connected Lol", Toast.LENGTH_SHORT).show();
                                updateLoginTime(System.currentTimeMillis());
                            } catch (Exception ignored) {
                            }
                            wb.loadUrl("https://gstatic.com/generate_204");
                        } else if (!url.contains("logout")) {
                            wb.evaluateJavascript("/authentication.{0,3}failed/i.test(document.body.innerHTML)", (String s) -> {
                                if (s.equals("true")) {
                                    updateLoginTime(-1);
                                    notifyWrongPass(NotificationListener.this);
                                }else{
                                    wb.evaluateJavascript("document.querySelector('#ft_un').value='" + user + "';document.querySelector('#ft_pd').value='" + pass + "';document.querySelector('div.fer').querySelector('input').click();",null);
                                }
                            });
                        }
                        super.onPageFinished(view, url);
                    }
                });
                wb.loadUrl("http://192.168.249.1:1000/logout?overhai");
            }
        }
    }
    private void updateLoginTime(long t){
        SharedPreferences.Editor speditor = getSharedPreferences(getString(R.string.defeaultPrefs), Context.MODE_PRIVATE).edit();
        speditor.putLong(getString(R.string.prefs_login_time), t);
        speditor.apply();
        Intent intent = new Intent(getString(R.string.configactivity_broadcast_receiver));
        intent.putExtra(getString(R.string.login_time_extra),t);
        LocalBroadcastManager.getInstance(NotificationListener.this).sendBroadcast(intent);
    }
}
