package com.pmp.quiz;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.learn.ReviewManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quiz du parcours d'apprentissage. Types :
 * - pratique       : exercices libres, sans enjeu
 * - comprehension  : test de remédiation (80%) — débloque le droit de repasser l'examen
 * - examen_sous    : examen du sous-niveau (80%) — échec = verrou 24h + remédiation
 * - examen_niveau  : examen de passage du niveau (80%) — échec = verrou 24h
 */
public class ExamActivity extends AppCompatActivity {

    private ProgressManager progress;
    private ReviewManager reviewManager;
    private int erreursAjoutees = 0;
    private String type;
    private String subId;
    private int niveauId;
    private List<ContentManager.QItem> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private boolean answered = false;

    private TextView tvQuestion, tvProgress, tvFeedback, tvMode;
    private LinearLayout optionsContainer;
    private Button btnNext;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        progress = new ProgressManager(this);
        reviewManager = new ReviewManager(this);
        type = getIntent().getStringExtra("type");
        subId = getIntent().getStringExtra("subId");
        niveauId = getIntent().getIntExtra("niveauId", -1);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvMode = findViewById(R.id.tvMode);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadQuestions();
        if (questions.isEmpty()) { finish(); return; }

        tvMode.setText(modeLabel());
        btnNext.setOnClickListener(v -> {
            if (!answered) return;
            currentIndex++;
            showQuestion();
        });

        showQuestion();
    }

    private String modeLabel() {
        switch (type) {
            case "pratique": return "🏋️ Exercices de pratique";
            case "comprehension": return "📚 Test de compréhension (80%)";
            case "examen_sous": return "🎓 Examen du sous-niveau (80%)";
            case "examen_niveau": return "🎓 EXAMEN DE PASSAGE — Niveau " + niveauId + " (80%)";
            default: return "";
        }
    }

    private void loadQuestions() {
        if ("examen_niveau".equals(type)) {
            ContentManager.Niveau niveau = ContentManager.findNiveau(this, niveauId);
            if (niveau == null) return;
            List<ContentManager.QItem> pool = new ArrayList<>();
            for (ContentManager.SousNiveau s : niveau.sousNiveaux) pool.addAll(s.examen);
            Collections.shuffle(pool);
            int count = Math.min(15, pool.size());
            questions = new ArrayList<>(pool.subList(0, count));
        } else {
            ContentManager.SousNiveau sub = ContentManager.findSousNiveau(this, subId);
            if (sub == null) return;
            if ("pratique".equals(type)) {
                questions = new ArrayList<>(sub.pratique);
                Collections.shuffle(questions);
            } else if ("comprehension".equals(type)) {
                List<ContentManager.QItem> pool = new ArrayList<>(sub.pratique);
                pool.addAll(sub.examen);
                Collections.shuffle(pool);
                int count = Math.min(5, pool.size());
                questions = new ArrayList<>(pool.subList(0, count));
            } else { // examen_sous
                questions = new ArrayList<>(sub.examen);
                Collections.shuffle(questions);
            }
        }
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            showResult();
            return;
        }
        ContentManager.QItem q = questions.get(currentIndex);
        tvQuestion.setText(q.q);
        tvProgress.setText((currentIndex + 1) + "/" + questions.size());
        progressBar.setProgress((currentIndex + 1) * 100 / questions.size());

        optionsContainer.removeAllViews();
        for (int i = 0; i < q.options.length; i++) {
            Button btn = new Button(this);
            btn.setText((char) ('A' + i) + ". " + q.options[i]);
            btn.setTag(i);
            btn.setPadding(24, 16, 24, 16);
            btn.setTextSize(15);
            btn.setAllCaps(false);
            btn.setOnClickListener(v -> checkAnswer((int) v.getTag()));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);
            btn.setLayoutParams(params);
            optionsContainer.addView(btn);
        }
        tvFeedback.setVisibility(View.GONE);
        btnNext.setEnabled(false);
        answered = false;
    }

    private void checkAnswer(int selected) {
        if (answered) return;
        answered = true;
        ContentManager.QItem q = questions.get(currentIndex);
        boolean correct = selected == q.correct;
        if (correct) {
            score++;
        } else {
            // Répétition espacée : la question ratée entre en révision
            reviewManager.addFailure(q.q, q.options, q.correct, q.explication);
            erreursAjoutees++;
        }

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            Button btn = (Button) optionsContainer.getChildAt(i);
            btn.setEnabled(false);
            if (i == q.correct)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            else if (i == selected)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }

        tvFeedback.setText((correct ? "✅ Correct ! " : "❌ Incorrect. ") + q.explication);
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setEnabled(true);
        btnNext.setText(currentIndex == questions.size() - 1 ? "🏁 Voir le résultat" : "➡️ Suivant");
    }

    private void showResult() {
        int pct = (score * 100) / questions.size();
        boolean reussi = pct >= ProgressManager.SEUIL_REUSSITE;

        progress.recordStudy();

        String titre;
        String message;

        switch (type) {
            case "pratique":
                titre = pct >= 80 ? "💪 Excellente pratique !" : "📚 Continuez à vous exercer";
                message = "Score : " + score + "/" + questions.size() + " (" + pct + "%)\n\n"
                        + "Les exercices de pratique sont illimités. Quand vous vous sentez prêt, passez l'examen du sous-niveau.";
                break;

            case "comprehension":
                if (reussi) {
                    progress.setComprehensionOk(subId);
                    titre = "✅ Compréhension validée !";
                    message = "Score : " + pct + "%\n\nVous pouvez repasser l'examen"
                            + (progress.isLocked("sub_" + subId)
                            ? " dès la fin du délai de 24h (" + progress.getRemainingLockText("sub_" + subId) + " restants)."
                            : " dès maintenant !");
                } else {
                    titre = "📚 Pas encore acquis";
                    message = "Score : " + pct + "% (80% requis)\n\nRelisez la leçon attentivement puis retentez le test de compréhension. Les tentatives sont illimitées.";
                }
                break;

            case "examen_sous":
                if (reussi) {
                    progress.setSousNiveauValide(subId, pct);
                    titre = "🎉 Sous-niveau validé !";
                    message = "Score : " + pct + "%\n\nFélicitations ! L'étape suivante est maintenant débloquée.";
                } else {
                    progress.lockExam("sub_" + subId);
                    progress.setRemediation(subId);
                    titre = "❌ Examen non validé";
                    message = "Score : " + pct + "% (80% requis)\n\n"
                            + "Plan de remédiation :\n"
                            + "1. Relisez la leçon (ou écoutez-la)\n"
                            + "2. Validez le test de compréhension\n"
                            + "3. Repassez l'examen dans 24h\n\n"
                            + "Ce délai vous aide à vraiment assimiler — c'est comme ça qu'on réussit le vrai PMP !";
                }
                break;

            case "examen_niveau":
                if (reussi) {
                    progress.setNiveauValide(niveauId, pct);
                    titre = "👑 NIVEAU " + niveauId + " VALIDÉ !";
                    message = "Score : " + pct + "%\n\nBravo, le niveau suivant est débloqué !";
                } else {
                    progress.lockExam("niveau_" + niveauId);
                    titre = "❌ Examen de passage non validé";
                    message = "Score : " + pct + "% (80% requis)\n\n"
                            + "Revoyez les sous-niveaux où vous avez fait des erreurs, puis réessayez dans 24h.";
                }
                break;

            default:
                titre = "Résultat";
                message = "Score : " + pct + "%";
        }

        // Incitation à la révision répétitive
        if (erreursAjoutees > 0) {
            message += "\n\n🔁 " + erreursAjoutees + " question(s) ratée(s) ajoutée(s) à votre Révision du jour : refaites-les demain pour les ancrer durablement. La répétition est la clé !";
        }

        // Affichage du résultat en remplaçant le contenu
        tvQuestion.setText(titre);
        tvProgress.setText("");
        progressBar.setProgress(100);
        optionsContainer.removeAllViews();
        tvFeedback.setText(message);
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setText("🏠 Retour au parcours");
        btnNext.setEnabled(true);
        btnNext.setOnClickListener(v -> finish());
    }
}
