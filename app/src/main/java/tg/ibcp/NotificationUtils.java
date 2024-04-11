package tg.ibcp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    public static void notifyCredentialsNotSet(Context context){
        CharSequence name = "Credentials Setting";
        String description = "Reminder to complete setup";
        String id = "notifyCredentialsNotSet";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, ConfigActivity.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, id)
                        .setSmallIcon(R.drawable.baseline_wifi_lock_24)
                        .setContentTitle("WiFi Login Credentials Not Set")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setContentText("Tap to setup automatic wifi login.");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(ConfigActivity.NOTIF_ID_SETCREDS, builder.build());
    }
    public static void notifyWrongPass(Context context){
        CharSequence name = "Wrong Credentials";
        String description = "Notify about wrong username/password";
        String id = "notifyWrongPass";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, ConfigActivity.class), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, id)
                        .setSmallIcon(R.drawable.baseline_wifi_lock_24)
                        .setContentTitle("Wrong WiFi Login Password")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setContentText("Tap to re-enter automatic wifi login credentials.");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(ConfigActivity.NOTIF_ID_WRONGPASS, builder.build());
    }
}
