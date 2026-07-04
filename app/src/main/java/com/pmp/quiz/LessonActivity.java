package com.pmp.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ProgressManager;
import java.util.Locale;

/** Hub du sous-niveau : leçon (lecture + audio TTS), pratique, test de compréhension, examen */
public class LessonActivity extends AppCompatActivity {

    private ProgressManager progress;
    private ContentManager.SousNiveau sub;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean speaking = false;
    private Button btnAudio, btnLeconLue, btnPratique, btnComprehension, btnExamen;
    private TextView tvExamenInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        progress = new ProgressManager(this);
        String subId = getIntent().getStringExtra("subId");
        sub = ContentManager.findSousNiveau(this, subId);
        if (sub == null) { finish(); return; }

        TextView tvTitle = findViewById(R.id.tvLessonTitle);
        TextView tvContent = findViewById(R.id.tvLessonContent);
        tvTitle.setText(sub.id + " — " + sub.titre);
        tvContent.setText(sub.lecon);

        btnAudio = findViewById(R.id.btnAudio);
        btnLeconLue = findViewById(R.id.btnLeconLue);
        btnPratique = findViewById(R.id.btnPratique);
        btnComprehension = findViewById(R.id.btnComprehension);
        btnExamen = findViewById(R.id.btnExamen);
        tvExamenInfo = findViewById(R.id.tvExamenInfo);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Synthèse vocale (fonctionne hors ligne si moteur TTS français installé)
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.FRENCH);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        btnAudio.setOnClickListener(v -> toggleAudio());

        btnLeconLue.setOnClickListener(v -> {
            progress.setLeconLue(sub.id);
            Toast.makeText(this, "Leçon marquée comme lue ✓", Toast.LENGTH_SHORT).show();
            refreshButtons();
        });

        btnPratique.setOnClickListener(v -> launchExam("pratique"));
        btnComprehension.setOnClickListener(v -> launchExam("comprehension"));
        btnExamen.setOnClickListener(v -> launchExam("examen_sous"));
    }

    private void launchExam(String type) {
        Intent i = new Intent(this, ExamActivity.class);
        i.putExtra("type", type);
        i.putExtra("subId", sub.id);
        startActivity(i);
    }

    private void toggleAudio() {
        if (!ttsReady) {
            Toast.makeText(this, "Synthèse vocale française non disponible sur cet appareil", Toast.LENGTH_LONG).show();
            return;
        }
        if (speaking) {
            tts.stop();
            speaking = false;
            btnAudio.setText("🔊 Écouter la leçon");
        } else {
            String texte = sub.titre + ". " + sub.lecon;
            // Découper en morceaux (limite TTS ~4000 caractères)
            int chunk = 3500;
            for (int start = 0; start < texte.length(); start += chunk) {
                String part = texte.substring(start, Math.min(texte.length(), start + chunk));
                tts.speak(part, start == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null,
                        "lecon_" + start);
            }
            speaking = true;
            btnAudio.setText("⏹️ Arrêter la lecture");
        }
    }

    private void refreshButtons() {
        boolean leconLue = progress.isLeconLue(sub.id);
        boolean valide = progress.isSousNiveauValide(sub.id);
        boolean needRemed = progress.needsRemediation(sub.id) && !progress.isComprehensionOk(sub.id);

        btnLeconLue.setText(leconLue ? "✓ Leçon lue" : "✅ J'ai lu la leçon");
        btnLeconLue.setEnabled(!leconLue);

        btnPratique.setEnabled(leconLue);

        // Test de compréhension : visible uniquement en remédiation
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
        refreshButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null && speaking) {
            tts.stop();
            speaking = false;
            btnAudio.setText("🔊 Écouter la leçon");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
