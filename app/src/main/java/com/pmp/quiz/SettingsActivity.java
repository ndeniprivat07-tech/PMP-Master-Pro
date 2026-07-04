package com.pmp.quiz;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ReviewManager;
import com.pmp.quiz.services.NotificationReceiver;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView tvDateExamen, tvObjectif, tvRappel, tvVitesse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences("parametres", Context.MODE_PRIVATE);

        tvDateExamen = findViewById(R.id.tvDateExamen);
        tvObjectif = findViewById(R.id.tvObjectif);
        tvRappel = findViewById(R.id.tvRappel);
        tvVitesse = findViewById(R.id.tvVitesse);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowDateExamen).setOnClickListener(v -> choisirDateExamen());
        findViewById(R.id.rowObjectif).setOnClickListener(v -> choisirObjectif());
        findViewById(R.id.rowRappel).setOnClickListener(v -> choisirRappel());
        findViewById(R.id.rowVitesse).setOnClickListener(v -> choisirVitesse());
        findViewById(R.id.rowReset).setOnClickListener(v -> confirmerReset());
        findViewById(R.id.rowAPropos).setOnClickListener(v -> afficherAPropos());

        refresh();
    }

    private void refresh() {
        long dateExamen = prefs.getLong("date_examen", 0);
        if (dateExamen > 0) {
            long jours = (dateExamen - System.currentTimeMillis()) / (24L * 3600 * 1000);
            tvDateExamen.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(dateExamen)
                    + (jours >= 0 ? "  (J-" + jours + ")" : "  (passée)"));
        } else {
            tvDateExamen.setText("Non définie — touchez pour choisir");
        }

        int objectif = prefs.getInt("objectif_quotidien", 20);
        tvObjectif.setText(objectif + " questions par jour");

        boolean rappelOn = prefs.getBoolean("rappel_actif", false);
        int heure = prefs.getInt("rappel_heure", 19);
        int minute = prefs.getInt("rappel_minute", 0);
        tvRappel.setText(rappelOn
                ? String.format(Locale.FRANCE, "Activé — tous les jours à %02d:%02d", heure, minute)
                : "Désactivé — touchez pour activer");

        float vitesse = getSharedPreferences("tts_prefs", MODE_PRIVATE).getFloat("speech_rate", 1.0f);
        tvVitesse.setText("Vitesse de lecture : " + vitesse + "x");
    }

    private void choisirDateExamen() {
        Calendar c = Calendar.getInstance();
        long saved = prefs.getLong("date_examen", 0);
        if (saved > 0) c.setTimeInMillis(saved);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(y, m, d, 8, 0, 0);
            prefs.edit().putLong("date_examen", chosen.getTimeInMillis()).apply();
            long jours = (chosen.getTimeInMillis() - System.currentTimeMillis()) / (24L * 3600 * 1000);
            Toast.makeText(this, "Objectif fixé : J-" + jours + " — travaillez chaque jour !", Toast.LENGTH_LONG).show();
            refresh();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void choisirObjectif() {
        final String[] labels = {"10 questions/jour (léger)", "20 questions/jour (recommandé)",
                "30 questions/jour (intensif)", "50 questions/jour (sprint final)"};
        final int[] valeurs = {10, 20, 30, 50};
        int current = prefs.getInt("objectif_quotidien", 20);
        int checked = 1;
        for (int i = 0; i < valeurs.length; i++) if (valeurs[i] == current) checked = i;
        new AlertDialog.Builder(this)
                .setTitle("🎯 Objectif quotidien")
                .setSingleChoiceItems(labels, checked, (d, w) -> {
                    prefs.edit().putInt("objectif_quotidien", valeurs[w]).apply();
                    d.dismiss();
                    refresh();
                }).show();
    }

    private void choisirRappel() {
        boolean actif = prefs.getBoolean("rappel_actif", false);
        if (actif) {
            new AlertDialog.Builder(this)
                    .setTitle("🔔 Rappel quotidien")
                    .setMessage("Le rappel est activé. Que voulez-vous faire ?")
                    .setPositiveButton("Changer l'heure", (d, w) -> choisirHeureRappel())
                    .setNegativeButton("Désactiver", (d, w) -> {
                        prefs.edit().putBoolean("rappel_actif", false).apply();
                        annulerRappel();
                        refresh();
                    })
                    .setNeutralButton("Annuler", null)
                    .show();
        } else {
            choisirHeureRappel();
        }
    }

    private void choisirHeureRappel() {
        // Android 13+ : demander la permission d'afficher des notifications
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        int heure = prefs.getInt("rappel_heure", 19);
        int minute = prefs.getInt("rappel_minute", 0);
        new TimePickerDialog(this, (view, h, m) -> {
            prefs.edit().putBoolean("rappel_actif", true)
                    .putInt("rappel_heure", h).putInt("rappel_minute", m).apply();
            programmerRappel(h, m);
            Toast.makeText(this, String.format(Locale.FRANCE, "🔔 Rappel quotidien programmé à %02d:%02d", h, m),
                    Toast.LENGTH_LONG).show();
            refresh();
        }, heure, minute, true).show();
    }

    private void programmerRappel(int heure, int minute) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, heure);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    private void annulerRappel() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    private void choisirVitesse() {
        final String[] labels = {"🐢 Lente (0.75x)", "Normale (1x)", "Rapide (1.25x)", "🐇 Très rapide (1.5x)"};
        final float[] rates = {0.75f, 1.0f, 1.25f, 1.5f};
        SharedPreferences tts = getSharedPreferences("tts_prefs", MODE_PRIVATE);
        float current = tts.getFloat("speech_rate", 1.0f);
        int checked = 1;
        for (int i = 0; i < rates.length; i++) if (Math.abs(rates[i] - current) < 0.01f) checked = i;
        new AlertDialog.Builder(this)
                .setTitle("🔊 Vitesse de lecture audio")
                .setSingleChoiceItems(labels, checked, (d, w) -> {
                    tts.edit().putFloat("speech_rate", rates[w]).apply();
                    d.dismiss();
                    refresh();
                }).show();
    }

    private void confirmerReset() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Réinitialiser la progression")
                .setMessage("Tout sera effacé : parcours validés, révisions, streak, historique d'examens.\n\nCette action est IRRÉVERSIBLE. Continuer ?")
                .setPositiveButton("Continuer", (d, w) -> new AlertDialog.Builder(this)
                        .setTitle("Dernière confirmation")
                        .setMessage("Vraiment tout effacer ?")
                        .setPositiveButton("OUI, tout effacer", (d2, w2) -> {
                            for (String p : new String[]{"progression", "revision", "examens_blancs", "quiz_session"}) {
                                getSharedPreferences(p, MODE_PRIVATE).edit().clear().apply();
                            }
                            Toast.makeText(this, "Progression réinitialisée", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Non", null).show())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void afficherAPropos() {
        int total = 0;
        for (ContentManager.Niveau n : ContentManager.getNiveaux(this)) {
            for (ContentManager.SousNiveau s : n.sousNiveaux) {
                total += s.pratique.size() + s.examen.size();
            }
        }
        total += ContentManager.getBank(this).size();
        int enRevision = new ReviewManager(this).getTotalCount();
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ PMP Master Pro")
                .setMessage("Version 2.0\n\n"
                        + "📚 " + total + " questions rédigées (banque en croissance continue)\n"
                        + "🧮 Générateur de calculs : questions illimitées\n"
                        + "🔁 " + enRevision + " question(s) dans votre révision\n\n"
                        + "Basé sur le PMBOK 7e édition, l'ECO 2021 et l'Agile Practice Guide.\n\n"
                        + "Fonctionne entièrement sans connexion internet.")
                .setPositiveButton("Fermer", null)
                .show();
    }
}
