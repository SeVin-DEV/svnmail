package com.svndev.svnmail;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String SERVICE_CHANNEL = "mail_monitor";
    public static final String MAIL_CHANNEL = "new_mail";

    public static void createChannels(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(SERVICE_CHANNEL,
                "Mail Monitor", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(MAIL_CHANNEL,
                "New Mail", NotificationManager.IMPORTANCE_HIGH));
    }

    public static android.app.Notification serviceNotification(Context ctx, int accountCount) {
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(ctx, SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(ctx.getString(R.string.app_name))
                .setContentText(ctx.getString(R.string.monitoring) +
                        " (" + accountCount + " account" + (accountCount == 1 ? "" : "s") + ")")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    public static void notifyNewMail(Context ctx, Account account, String from,
                                     String subject) {
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx,
                (int) account.id, open, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, MAIL_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(from)
                .setContentText(subject != null && !subject.isEmpty() ? subject : "(no subject)")
                .setSubText(account.email)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.notify((int) (1000 + account.id), b.build());
    }
}
