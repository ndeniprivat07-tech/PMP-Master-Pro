package com.pmp.quiz.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Reprogramme le rappel quotidien après un redémarrage du téléphone (les alarmes sont effacées au reboot). */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("parametres", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("rappel_actif", false)) return;

        int heure = prefs.getInt("rappel_heure", 19);
        int minute = prefs.getInt("rappel_minute", 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notif = new Intent(context, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 100, notif,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, heure);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }
}
