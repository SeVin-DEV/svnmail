package com.svndev.svnmail;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

public class MailPollService extends Service {
    private static final long POLL_INTERVAL_MS = 60_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            pollAllAccounts();
            if (running) handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DatabaseHelper db = new DatabaseHelper(this);
        List<Account> accounts = db.getAccounts();
        startForeground(1, NotificationHelper.serviceNotification(this, accounts.size()));
        if (!running) {
            running = true;
            handler.post(pollTask);
        }
        return START_STICKY;
    }

    private void pollAllAccounts() {
        new Thread(() -> {
            DatabaseHelper db = new DatabaseHelper(this);
            List<Account> accounts = db.getAccounts();
            for (Account acc : accounts) {
                try {
                    long lastUid = db.getLastUid(acc.id);
                    long maxUid = MailClient.maxUid(acc);
                    if (maxUid > lastUid && lastUid > 0) {
                        javax.mail.Message newest = MailClient.getNewestMessage(acc);
                        if (newest != null) {
                            String from = newest.getFrom() != null &&
                                    newest.getFrom().length > 0
                                    ? newest.getFrom()[0].toString() : acc.email;
                            String subject = newest.getSubject() != null
                                    ? newest.getSubject() : "(no subject)";
                            NotificationHelper.notifyNewMail(this, acc, from, subject);
                        }
                    }
                    db.setLastUid(acc.id, Math.max(lastUid, maxUid));
                } catch (Exception ignored) {
                }
            }
            startForeground(1, NotificationHelper.serviceNotification(this, accounts.size()));
        }).start();
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(pollTask);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
