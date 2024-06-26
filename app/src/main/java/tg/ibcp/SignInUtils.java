package tg.ibcp;

import static tg.ibcp.NotificationUtils.notifyCredentialsNotSet;
import static tg.ibcp.NotificationUtils.notifyWrongPass;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignInUtils {
    public static final String GENERATE_204_URL = "https://gstatic.com/generate_204";

    public static void isIntendedWiFiNetwork(LinkProperties linkprops, final ValueCallback<Boolean> vc){
        if(linkprops!=null && linkprops.getDomains()!=null && linkprops.getDomains().toLowerCase().contains("iitbhu.local")){
            vc.onReceiveValue(true);
        }else{
            new Thread(()->{
                try{
                    HttpURLConnection con =(HttpURLConnection)new URL("http://192.168.249.1:1000/login?hi").openConnection();
                    con.setConnectTimeout(1500);
                    vc.onReceiveValue(con.getResponseCode()/100==2);
                }catch (IOException icecream){
                    vc.onReceiveValue(false);
                }
            }).start();
        }
    }
    public static void signInIfRequired(LinkProperties linkprops, Context ctx){
        isIntendedWiFiNetwork(linkprops, answer -> {
            if(answer){
                try{
                    int resCode = ((HttpURLConnection)new URL(SignInUtils.GENERATE_204_URL).openConnection()).getResponseCode();
                    if(resCode!=204){
                        signInNonLooper(ctx);
                    }
                }catch (Exception ignored){
                    signInNonLooper(ctx);
                }

            }
        });
    }
    public static void signInNonLooper(Context ctx){
        new Handler(Looper.getMainLooper()).post(() -> {
                    signIn(ctx);
                }
        );
    }
    public static void signIn(Context ctx){
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(ctx.getString(R.string.defeaultPrefs), Context.MODE_PRIVATE);
        final String user = StringEscapeUtils.escapeEcmaScript(sp.getString(ctx.getString(R.string.prefs_user),""));
        final String pass = StringEscapeUtils.escapeEcmaScript(sp.getString(ctx.getString(R.string.prefs_pass),""));
        final long loginTime = sp.getLong(ctx.getString(R.string.prefs_login_time),0);
        if(pass.length()==0){
            notifyCredentialsNotSet(ctx);
        }else if(loginTime==-1){
            notifyWrongPass(ctx);
        }else{
            WebView wb = new WebView(ctx);
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
                            Toast.makeText(ctx, "Connected Lol", Toast.LENGTH_SHORT).show();
                            updateLoginTime(ctx, System.currentTimeMillis());
                        } catch (Exception ignored) {
                        }
                        wb.loadUrl(GENERATE_204_URL);
                    }
                    wb.evaluateJavascript("/authentication.{0,3}failed/i.test(document.body.innerHTML)", (String s) -> {
                        if (s.equals("true")) {
                            updateLoginTime(ctx, -1);
                            notifyWrongPass(ctx);
                        }else if(!url.contains("logout")){
                            wb.evaluateJavascript("document.querySelector('#ft_un').value='" + user + "';document.querySelector('#ft_pd').value='" + pass + "';document.querySelector('div.fer').querySelector('input').click();",null);
                        }
                    });
                    super.onPageFinished(view, url);
                }
            });
            wb.loadUrl("http://192.168.249.1:1000/logout?overhai");
        }
    }
    public static void updateLoginTime(Context ctx, long t){
        SharedPreferences.Editor speditor = ctx.getSharedPreferences(ctx.getString(R.string.defeaultPrefs), Context.MODE_PRIVATE).edit();
        speditor.putLong(ctx.getString(R.string.prefs_login_time), t);
        speditor.apply();
        Intent intent = new Intent(ctx.getString(R.string.configactivity_broadcast_receiver));
        intent.putExtra(ctx.getString(R.string.login_time_extra),t);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }
}
