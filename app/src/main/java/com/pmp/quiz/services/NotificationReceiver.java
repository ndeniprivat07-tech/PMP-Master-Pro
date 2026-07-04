package com.pmp.quiz.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.pmp.quiz.MainActivity;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.learn.ReviewManager;

/** Rappel quotidien d'étude : entretient le streak et pousse la révision du jour. */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "rappel_quotidien";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    "Rappel quotidien", NotificationManager.IMPORTANCE_DEFAULT));
        }

        int due = new ReviewManager(context).getDueCount();
        int streak = new ProgressManager(context).getStreak();

        String texte;
        if (due > 0) {
            texte = "🔁 " + due + " question(s) vous attendent en révision. "
                    + (streak > 0 ? "Ne cassez pas votre série de " + streak + " jour(s) ! 🔥" : "C'est le moment idéal !");
        } else {
            texte = (streak > 0 ? "🔥 Série de " + streak + " jour(s) en cours — " : "")
                    + "Quelques questions aujourd'hui et vous vous rapprochez du PMP ! 💪";
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("📘 PMP Master Pro — c'est l'heure d'étudier")
                .setContentText(texte)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texte))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        nm.notify(200, notif);
    }
}
