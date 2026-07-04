package com.pmp.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.pmp.quiz.database.DatabaseHelper;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.learn.ReviewManager;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvProgress, tvLevel, tvQuestionsAnswered, tvSuccessRate, tvStreak;
    private CardView cardTraining, cardExam, cardLearn, cardStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadStats();
        setupClickListeners();
    }

    private void initViews() {
        tvProgress = findViewById(R.id.tvProgress);
        tvLevel = findViewById(R.id.tvLevel);
        tvQuestionsAnswered = findViewById(R.id.tvQuestionsAnswered);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvStreak = findViewById(R.id.tvStreak);
        cardTraining = findViewById(R.id.cardTraining);
        cardExam = findViewById(R.id.cardExam);
        cardLearn = findViewById(R.id.cardLearn);
        cardStats = findViewById(R.id.cardStats);
    }

    private void loadStats() {
        // Métriques unifiées : toutes les activités comptent (parcours, entraînement, révision, calculs)
        ProgressManager pm = new ProgressManager(this);
        int total = pm.getTotalQuestions();
        int correct = pm.getTotalCorrect();

        tvQuestionsAnswered.setText(String.valueOf(total));
        int successRate = total > 0 ? (correct * 100) / total : 0;
        tvSuccessRate.setText(successRate + "%");
        int totalSubs = 0, valides = 0;
        for (com.pmp.quiz.learn.ContentManager.Niveau n :
                com.pmp.quiz.learn.ContentManager.getNiveaux(this)) {
            for (com.pmp.quiz.learn.ContentManager.SousNiveau s : n.sousNiveaux) {
                totalSubs++;
                if (pm.isSousNiveauValide(s.id)) valides++;
            }
        }
        int progress = totalSubs > 0 ? (valides * 100) / totalSubs : 0;
        tvProgress.setText(progress + "%");

        String level;
        if (total < 50) level = "Débutant";
        else if (total < 200) level = "Intermédiaire";
        else if (total < 500) level = "Avancé";
        else if (total < 1000) level = "Expert";
        else level = "Maître";
        tvLevel.setText(level);

        int streak = new ProgressManager(this).getStreak();
        tvStreak.setText("🔥 " + streak + (streak > 1 ? " jours" : " jour"));
    }

    private void setupClickListeners() {
        cardTraining.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("mode", "entrainement");
            startActivity(intent);
        });

        cardExam.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("mode", "examen");
            startActivity(intent);
        });

        cardLearn.setOnClickListener(v -> startActivity(new Intent(this, LearnActivity.class)));

        findViewById(R.id.cardReview).setOnClickListener(v ->
                startActivity(new Intent(this, ReviewActivity.class)));

        findViewById(R.id.cardCalc).setOnClickListener(v ->
                startActivity(new Intent(this, CalcActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        cardStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        updateReviewBadge();
        updateJournee();
    }

    /** Bandeau directif : dit à l'utilisateur QUOI faire aujourd'hui */
    private void updateJournee() {
        android.widget.TextView tvJournee = findViewById(R.id.tvJournee);
        ProgressManager pm = new ProgressManager(this);
        android.content.SharedPreferences params = getSharedPreferences("parametres", MODE_PRIVATE);

        StringBuilder sb = new StringBuilder("📋 AUJOURD'HUI");

        // Compte à rebours de l'examen
        long dateExamen = params.getLong("date_examen", 0);
        if (dateExamen > 0) {
            long jours = (dateExamen - System.currentTimeMillis()) / (24L * 3600 * 1000);
            if (jours >= 0) sb.append("  •  🎯 J-").append(jours);
        }

        // Objectif quotidien mesuré
        int objectif = params.getInt("objectif_quotidien", 20);
        int faites = pm.getTodayCount();
        sb.append("\n🏋️ Objectif : ").append(faites).append("/").append(objectif).append(" questions");
        if (faites >= objectif) sb.append(" ✅ atteint, bravo !");
        else sb.append(" — encore ").append(objectif - faites);

        // Révisions dues
        int due = new ReviewManager(this).getDueCount();
        if (due > 0) sb.append("\n🔁 ").append(due).append(" question(s) à réviser en priorité");

        tvJournee.setText(sb.toString());
    }

    /** Compteur de révision : incite l'utilisateur à s'exercer chaque jour */
    private void updateReviewBadge() {
        ReviewManager rm = new ReviewManager(this);
        int due = rm.getDueCount();
        int total = rm.getTotalCount();
        android.widget.TextView tvReviewCount = findViewById(R.id.tvReviewCount);
        if (due > 0) {
            tvReviewCount.setText("⚡ " + due + " question(s) à réviser MAINTENANT — allez-y !");
        } else if (total > 0) {
            tvReviewCount.setText(total + " question(s) en mémoire — revenez demain, faites des exercices aujourd'hui");
        } else {
            tvReviewCount.setText("Vos erreurs reviendront ici à intervalles espacés");
        }
    }
}
