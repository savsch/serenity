package tg.ibcp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;


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
            SignInUtils.signIn(NotificationListener.this);
        }
    }

}
