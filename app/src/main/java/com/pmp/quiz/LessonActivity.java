package com.pmp.quiz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.services.TtsService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Leçon découpée en sections (1.1.1, 1.1.2...) naviguables une par une.
 * Audio en arrière-plan (continue écran verrouillé) avec choix de voix et de vitesse.
 */
public class LessonActivity extends AppCompatActivity {

    private ProgressManager progress;
    private ContentManager.SousNiveau sub;
    private List<ContentManager.Section> sections;
    private int currentSection = 0;

    private TextView tvLessonTitle, tvSectionTitle, tvLessonContent, tvSecCounter, tvExamenInfo;
    private Button btnAudio, btnVoice, btnSpeed, btnPrevSec, btnNextSec;
    private Button btnPratique, btnComprehension, btnExamen;

    private SharedPreferences lessonPrefs;
    private SharedPreferences ttsPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        progress = new ProgressManager(this);
        lessonPrefs = getSharedPreferences("progression", Context.MODE_PRIVATE);
        ttsPrefs = getSharedPreferences("tts_prefs", Context.MODE_PRIVATE);

        String subId = getIntent().getStringExtra("subId");
        sub = ContentManager.findSousNiveau(this, subId);
        if (sub == null) { finish(); return; }
        sections = sub.getSections();

        tvLessonTitle = findViewById(R.id.tvLessonTitle);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvLessonContent = findViewById(R.id.tvLessonContent);
        tvSecCounter = findViewById(R.id.tvSecCounter);
        tvExamenInfo = findViewById(R.id.tvExamenInfo);
        btnAudio = findViewById(R.id.btnAudio);
        btnVoice = findViewById(R.id.btnVoice);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnPrevSec = findViewById(R.id.btnPrevSec);
        btnNextSec = findViewById(R.id.btnNextSec);
        btnPratique = findViewById(R.id.btnPratique);
        btnComprehension = findViewById(R.id.btnComprehension);
        btnExamen = findViewById(R.id.btnExamen);

        tvLessonTitle.setText(sub.id + " — " + sub.titre);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnPrevSec.setOnClickListener(v -> showSection(currentSection - 1));
        btnNextSec.setOnClickListener(v -> showSection(currentSection + 1));
        btnAudio.setOnClickListener(v -> toggleAudio());
        btnVoice.setOnClickListener(v -> chooseVoice());
        btnSpeed.setOnClickListener(v -> chooseSpeed());
        btnPratique.setOnClickListener(v -> launchExam("pratique"));
        btnComprehension.setOnClickListener(v -> launchExam("comprehension"));
        btnExamen.setOnClickListener(v -> launchExam("examen_sous"));

        // Reprendre à la première section non lue
        int first = 0;
        for (int i = 0; i < sections.size(); i++) {
            if (!isSectionRead(i)) { first = i; break; }
            if (i == sections.size() - 1) first = i;
        }
        showSection(first);
    }

    // ===== Sections =====

    private boolean isSectionRead(int index) {
        return lessonPrefs.getBoolean("lecon_sec_" + sub.id + "_" + index, false);
    }

    private void markSectionRead(int index) {
        lessonPrefs.edit().putBoolean("lecon_sec_" + sub.id + "_" + index, true).apply();
        // Toutes les sections lues -> leçon lue
        for (int i = 0; i < sections.size(); i++) {
            if (!isSectionRead(i)) return;
        }
        if (!progress.isLeconLue(sub.id)) {
            progress.setLeconLue(sub.id);
            Toast.makeText(this, "📖 Leçon complète lue ✓ — exercices et examen débloqués !",
                    Toast.LENGTH_LONG).show();
        }
    }

    private android.os.CountDownTimer lectureTimer;

    private void showSection(int index) {
        if (index < 0 || index >= sections.size()) return;
        currentSection = index;
        ContentManager.Section s = sections.get(index);
        boolean dejaLue = isSectionRead(index);

        tvSectionTitle.setText(sub.id + "." + (index + 1) + "  " + s.titre + (dejaLue ? "  ✓" : ""));
        tvLessonContent.setText(s.texte);
        tvSecCounter.setText((index + 1) + "/" + sections.size());
        btnPrevSec.setEnabled(index > 0);

        if (lectureTimer != null) lectureTimer.cancel();
        final String labelSuivant = index == sections.size() - 1 ? "✓ Terminer" : "Suivant ▶";

        if (dejaLue) {
            // Section déjà lue : navigation libre
            btnNextSec.setEnabled(true);
            btnNextSec.setText(labelSuivant);
        } else {
            // Temps minimal de lecture : ~1 s pour 15 caractères (lecture réelle), entre 10 et 60 s
            int secondes = Math.max(10, Math.min(60, s.texte.length() / 15));
            btnNextSec.setEnabled(false);
            lectureTimer = new android.os.CountDownTimer(secondes * 1000L, 1000) {
                public void onTick(long ms) {
                    btnNextSec.setText("📖 Lecture... " + (ms / 1000) + " s");
                }
                public void onFinish() {
                    markSectionRead(currentSection);
                    tvSectionTitle.setText(sub.id + "." + (currentSection + 1) + "  "
                            + sections.get(currentSection).titre + "  ✓");
                    btnNextSec.setEnabled(true);
                    btnNextSec.setText(labelSuivant);
                    refreshButtons();
                }
            }.start();
        }

        if (index == sections.size() - 1) {
            btnNextSec.setOnClickListener(v -> {
                Toast.makeText(this, "Leçon terminée ! Passez aux exercices 🏋️", Toast.LENGTH_SHORT).show();
                refreshButtons();
            });
        } else {
            btnNextSec.setOnClickListener(v -> showSection(currentSection + 1));
        }
        refreshButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (lectureTimer != null) lectureTimer.cancel();
    }

    // ===== Audio (service en arrière-plan) =====

    private void toggleAudio() {
        if (TtsService.isPlaying) {
            Intent stop = new Intent(this, TtsService.class).setAction(TtsService.ACTION_STOP);
            startService(stop);
            btnAudio.setText("🔊 Écouter");
        } else {
            ContentManager.Section s = sections.get(currentSection);
            // Lire la section courante puis les suivantes (écoute continue)
            StringBuilder texte = new StringBuilder();
            for (int i = currentSection; i < sections.size(); i++) {
                texte.append(sections.get(i).titre).append(". ")
                     .append(sections.get(i).texte).append("\n\n");
            }
            Intent play = new Intent(this, TtsService.class)
                    .setAction(TtsService.ACTION_PLAY)
                    .putExtra(TtsService.EXTRA_TEXT, texte.toString())
                    .putExtra(TtsService.EXTRA_TITLE, sub.id + " — " + sub.titre);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(play);
            } else {
                startService(play);
            }
            btnAudio.setText("⏹️ Arrêter");
            Toast.makeText(this, "Lecture lancée — elle continue même écran verrouillé 🔒",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Choix de la voix =====

    private void chooseVoice() {
        Toast.makeText(this, "Recherche des voix disponibles...", Toast.LENGTH_SHORT).show();
        final TextToSpeech[] probe = new TextToSpeech[1];
        probe[0] = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(this, "Synthèse vocale indisponible", Toast.LENGTH_LONG).show();
                return;
            }
            List<Voice> frVoices = new ArrayList<>();
            if (probe[0].getVoices() != null) {
                for (Voice v : probe[0].getVoices()) {
                    if (v.getLocale().getLanguage().equals("fr") && !v.isNetworkConnectionRequired()) {
                        frVoices.add(v);
                    }
                }
            }
            if (frVoices.isEmpty()) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Aucune voix française installée")
                        .setMessage("Pour une voix plus naturelle :\n\n1. Ouvrez les Paramètres du téléphone\n2. Cherchez « Synthèse vocale »\n3. Choisissez « Synthèse vocale Google »\n4. Installez les données de voix françaises (qualité élevée)")
                        .setPositiveButton("Ouvrir les paramètres", (d, w) -> {
                            try {
                                startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
                            } catch (Exception e) {
                                Toast.makeText(this, "Ouvrez manuellement : Paramètres > Synthèse vocale", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Fermer", null)
                        .show());
                probe[0].shutdown();
                return;
            }
            Collections.sort(frVoices, (a, b) -> b.getQuality() - a.getQuality());
            String currentName = ttsPrefs.getString("voice_name", null);
            String[] labels = new String[frVoices.size()];
            int checked = 0;
            for (int i = 0; i < frVoices.size(); i++) {
                Voice v = frVoices.get(i);
                String qualite = v.getQuality() >= Voice.QUALITY_HIGH ? "★ haute qualité"
                        : v.getQuality() >= Voice.QUALITY_NORMAL ? "qualité normale" : "qualité basse";
                labels[i] = "Voix " + (i + 1) + " (" + v.getLocale().getDisplayCountry() + ", " + qualite + ")";
                if (v.getName().equals(currentName)) checked = i;
            }
            final int checkedFinal = checked;
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("🎙️ Choisir la voix")
                    .setSingleChoiceItems(labels, checkedFinal, (d, which) -> {
                        Voice v = frVoices.get(which);
                        ttsPrefs.edit().putString("voice_name", v.getName()).apply();
                        // Aperçu de la voix choisie
                        probe[0].setVoice(v);
                        probe[0].setSpeechRate(ttsPrefs.getFloat("speech_rate", 1.0f));
                        probe[0].speak("Bonjour, je serai votre voix pour les leçons PMP.",
                                TextToSpeech.QUEUE_FLUSH, null, "preview");
                    })
                    .setPositiveButton("Valider", (d, w) -> probe[0].shutdown())
                    .setOnDismissListener(d -> probe[0].shutdown())
                    .show());
        });
    }

    // ===== Choix de la vitesse =====

    private void chooseSpeed() {
        final String[] labels = {"🐢 Lente (0.75x)", "Normale (1x)", "Rapide (1.25x)", "🐇 Très rapide (1.5x)"};
        final float[] rates = {0.75f, 1.0f, 1.25f, 1.5f};
        float current = ttsPrefs.getFloat("speech_rate", 1.0f);
        int checked = 1;
        for (int i = 0; i < rates.length; i++) if (Math.abs(rates[i] - current) < 0.01f) checked = i;

        new AlertDialog.Builder(this)
                .setTitle("⏩ Vitesse de lecture")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    ttsPrefs.edit().putFloat("speech_rate", rates[which]).apply();
                    d.dismiss();
                    Toast.makeText(this, "Vitesse : " + labels[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ===== Boutons pratique / compréhension / examen =====

    private void launchExam(String type) {
        Intent i = new Intent(this, ExamActivity.class);
        i.putExtra("type", type);
        i.putExtra("subId", sub.id);
        startActivity(i);
    }

    private void refreshButtons() {
        boolean leconLue = progress.isLeconLue(sub.id);
        boolean valide = progress.isSousNiveauValide(sub.id);
        boolean needRemed = progress.needsRemediation(sub.id) && !progress.isComprehensionOk(sub.id);

        btnPratique.setEnabled(leconLue);
        btnComprehension.setVisibility(needRemed ? View.VISIBLE : View.GONE);

        if (valide) {
            btnExamen.setEnabled(false);
            btnExamen.setText("✅ Sous-niveau validé (" + progress.getScore(sub.id) + "%)");
            tvExamenInfo.setVisibility(View.GONE);
        } else if (progress.canTakeExamen(sub.id)) {
            btnExamen.setEnabled(true);
            btnExamen.setText("🎓 Passer l'examen (80% requis)");
            tvExamenInfo.setVisibility(View.GONE);
        } else {
            btnExamen.setEnabled(false);
            btnExamen.setText("🎓 Examen verrouillé");
            tvExamenInfo.setVisibility(View.VISIBLE);
            tvExamenInfo.setText(progress.getExamenBlockReason(sub.id));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnAudio.setText(TtsService.isPlaying ? "⏹️ Arrêter" : "🔊 Écouter");
        refreshButtons();
    }
}
