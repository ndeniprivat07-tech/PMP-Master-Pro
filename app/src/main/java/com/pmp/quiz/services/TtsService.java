package com.pmp.quiz.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

/**
 * Service de lecture audio des leçons en arrière-plan.
 * La lecture continue quand l'écran se verrouille ou quand on quitte l'app,
 * avec une notification permettant d'arrêter.
 */
public class TtsService extends Service {

    public static final String ACTION_PLAY = "com.pmp.quiz.TTS_PLAY";
    public static final String ACTION_STOP = "com.pmp.quiz.TTS_STOP";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_TITLE = "title";

    private static final String CHANNEL_ID = "tts_lecture";
    private static final int NOTIF_ID = 42;

    /** Indique aux activités si une lecture est en cours */
    public static volatile boolean isPlaying = false;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingText = null;
    private String pendingTitle = null;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                applyVoicePrefs();
                ttsReady = true;
                if (pendingText != null) {
                    speak(pendingTitle, pendingText);
                    pendingText = null;
                }
            } else {
                stopSelf();
            }
        });
    }

    /** Applique la voix et la vitesse choisies par l'utilisateur */
    private void applyVoicePrefs() {
        SharedPreferences prefs = getSharedPreferences("tts_prefs", Context.MODE_PRIVATE);
        int langResult = tts.setLanguage(Locale.FRENCH);
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault());
        }
        String voiceName = prefs.getString("voice_name", null);
        if (voiceName != null && tts.getVoices() != null) {
            for (Voice v : tts.getVoices()) {
                if (v.getName().equals(voiceName)) {
                    tts.setVoice(v);
                    break;
                }
            }
        }
        tts.setSpeechRate(prefs.getFloat("speech_rate", 1.0f));
        tts.setPitch(prefs.getFloat("pitch", 1.0f));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            return START_NOT_STICKY;
        }

        if (ACTION_PLAY.equals(action)) {
            String text = intent.getStringExtra(EXTRA_TEXT);
            String title = intent.getStringExtra(EXTRA_TITLE);
            if (text == null || text.isEmpty()) return START_NOT_STICKY;

            startForeground(NOTIF_ID, buildNotification(title));

            if (ttsReady) {
                applyVoicePrefs();
                speak(title, text);
            } else {
                pendingText = text;
                pendingTitle = title;
            }
        }
        return START_NOT_STICKY;
    }

    private void speak(String title, String text) {
        isPlaying = true;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onError(String utteranceId) {}
            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.endsWith("_last")) {
                    stopPlayback();
                }
            }
        });

        // Découper en morceaux (limite TTS ~4000 caractères)
        int chunk = 3500;
        int count = (text.length() + chunk - 1) / chunk;
        for (int i = 0; i < count; i++) {
            int start = i * chunk;
            String part = text.substring(start, Math.min(text.length(), start + chunk));
            String id = (i == count - 1) ? "lecon_" + i + "_last" : "lecon_" + i;
            tts.speak(part, i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, id);
        }
    }

    private void stopPlayback() {
        isPlaying = false;
        if (tts != null) tts.stop();
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification(String title) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Lecture des leçons", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }

        Intent stopIntent = new Intent(this, TtsService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("🔊 Lecture en cours")
                .setContentText(title != null ? title : "Leçon PMP")
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Arrêter", stopPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        isPlaying = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
