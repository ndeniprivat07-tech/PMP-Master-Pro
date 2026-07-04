package com.pmp.quiz;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.learn.ReviewManager;
import java.util.List;

/**
 * Révision du jour : rejoue les questions ratées selon la répétition espacée.
 * Réussie -> l'intervalle s'allonge (1j, 3j, 7j, 16j) ; ratée -> retour à demain.
 * 4 succès consécutifs = question maîtrisée, elle sort de la révision.
 */
public class ReviewActivity extends AppCompatActivity {

    private ReviewManager reviewManager;
    private ProgressManager progress;
    private List<ReviewManager.ReviewItem> items;
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

        reviewManager = new ReviewManager(this);
        progress = new ProgressManager(this);
        items = reviewManager.getDueItems();

        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvMode = findViewById(R.id.tvMode);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);

        tvMode.setText("🔁 Révision du jour");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (items.isEmpty()) {
            showEmpty();
            return;
        }

        btnNext.setOnClickListener(v -> {
            if (!answered) return;
            currentIndex++;
            showQuestion();
        });
        showQuestion();
    }

    private void showEmpty() {
        tvQuestion.setText("🎉 Rien à réviser aujourd'hui !");
        int total = reviewManager.getTotalCount();
        tvFeedback.setText(total > 0
                ? total + " question(s) reviendront bientôt selon leur échéance.\n\nEn attendant : faites des exercices de pratique dans le parcours ou un entraînement libre — plus vous vous exercez, plus vous progressez ! 💪"
                : "Aucune question en attente. Faites des exercices et des examens : chaque erreur alimentera automatiquement votre révision. C'est en répétant qu'on mémorise durablement ! 💪");
        tvFeedback.setVisibility(View.VISIBLE);
        progressBar.setProgress(100);
        btnNext.setText("🏠 Retour");
        btnNext.setEnabled(true);
        btnNext.setOnClickListener(v -> finish());
    }

    private void showQuestion() {
        if (currentIndex >= items.size()) {
            showResult();
            return;
        }
        ReviewManager.ReviewItem item = items.get(currentIndex);
        tvQuestion.setText(item.q);
        tvProgress.setText((currentIndex + 1) + "/" + items.size());
        progressBar.setProgress((currentIndex + 1) * 100 / items.size());

        optionsContainer.removeAllViews();
        for (int i = 0; i < item.options.length; i++) {
            if (item.options[i] == null) continue;
            Button btn = new Button(this);
            btn.setText((char) ('A' + i) + ". " + item.options[i]);
            btn.setTag(i);
            btn.setAllCaps(false);
            btn.setTextSize(15);
            btn.setPadding(24, 16, 24, 16);
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
        ReviewManager.ReviewItem item = items.get(currentIndex);
        boolean correct = selected == item.correct;

        if (correct) {
            score++;
            reviewManager.recordReviewSuccess(item.key);
        } else {
            reviewManager.recordReviewFailure(item.key);
        }

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            Button btn = (Button) optionsContainer.getChildAt(i);
            btn.setEnabled(false);
            int tag = (int) btn.getTag();
            if (tag == item.correct)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            else if (tag == selected)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }

        String suivi = correct
                ? (item.reps + 1 >= 4 ? "🏆 Question MAÎTRISÉE : elle sort de la révision !"
                                      : "✅ Bien ! Prochaine révision dans " + nextInterval(item.reps + 1) + ".")
                : "❌ Elle reviendra demain — c'est en répétant qu'on ancre la connaissance.";
        tvFeedback.setText((correct ? "✅ Correct ! " : "❌ Incorrect. ") + item.explication + "\n\n" + suivi);
        tvFeedback.setVisibility(View.VISIBLE);
        btnNext.setEnabled(true);
        btnNext.setText(currentIndex == items.size() - 1 ? "🏁 Terminer" : "➡️ Suivant");
    }

    private String nextInterval(int reps) {
        switch (reps) {
            case 1: return "3 jours";
            case 2: return "7 jours";
            default: return "16 jours";
        }
    }

    private void showResult() {
        progress.recordStudy();
        int pct = items.isEmpty() ? 0 : (score * 100) / items.size();
        int restant = reviewManager.getDueCount();
        tvQuestion.setText(pct >= 80 ? "💪 Excellente révision !" : "📚 Bonne séance de révision !");
        tvFeedback.setText("Score : " + score + "/" + items.size() + " (" + pct + "%)\n\n"
                + (restant > 0 ? "⚡ Encore " + restant + " question(s) à réviser — enchaînez tant que vous êtes lancé !"
                               : "Toutes vos révisions du jour sont faites ✅\n\nRevenez demain pour entretenir votre mémoire, et continuez les exercices du parcours pour engranger de nouvelles questions.")
                + "\n\n🔥 La régularité quotidienne est LE secret de la réussite au PMP.");
        tvFeedback.setVisibility(View.VISIBLE);
        progressBar.setProgress(100);
        optionsContainer.removeAllViews();
        btnNext.setEnabled(true);
        if (restant > 0) {
            btnNext.setText("🔁 Continuer la révision");
            btnNext.setOnClickListener(v -> {
                items = reviewManager.getDueItems();
                currentIndex = 0;
                score = 0;
                showQuestion();
            });
        } else {
            btnNext.setText("🏠 Retour");
            btnNext.setOnClickListener(v -> finish());
        }
    }
}
