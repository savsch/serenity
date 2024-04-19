package tg.ibcp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


public class NotificationListener extends NotificationListenerService {
    public static final String SSID = "IIT(BHU)";
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        if(
                sbn.getPackageName().equals("android")
                        && (
                                (
                                        sbn.getNotification().extras.getString("android.text")!=null
                                        && sbn.getNotification().extras.getString("android.text").contains(SSID)
                                ) || (
                                        // Xiaomi devices 🤢
                                        sbn.getNotification().extras.getString("android.title")!=null
                                        && sbn.getNotification().extras.getString("android.title").contains(SSID)
                                )
                        )
        ){
            try{
                sbn.getNotification().contentIntent.send();
            }catch (Exception ignored){
                NetworkUtils.bindProcessToWiFiNetwork((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE));
                SignInUtils.signIn(NotificationListener.this);
                try{
                    snoozeNotification(sbn.getKey(),288_00_000);
                } catch (Exception ignored2){}
            }

        }
    }

}
