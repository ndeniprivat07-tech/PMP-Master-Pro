package com.pmp.quiz;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.pmp.quiz.learn.CalcGenerator;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ProgressManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Entraînement aux calculs PMP : questions générées à l'infini, jamais deux fois les mêmes. */
public class CalcActivity extends AppCompatActivity {

    private ContentManager.QItem current;
    private int repondues = 0;
    private int correctes = 0;
    private boolean answered = false;
    private String categorie = CalcGenerator.CAT_TOUT;

    private static final String[] CAT_LABELS = {
            "💰 Économie / Coûts (CPI, SPI, EAC...)",
            "📅 Planning (chemin critique, marges, lag)",
            "⚖️ Risques et Décision (EMV, arbres de décision)",
            "⏱️ Estimations (PERT)",
            "📞 Communication (canaux)",
            "🎲 Tout mélangé"};
    private static final String[] CAT_VALUES = {
            CalcGenerator.CAT_ECONOMIE, CalcGenerator.CAT_PLANNING, CalcGenerator.CAT_DECISION,
            CalcGenerator.CAT_ESTIMATION, CalcGenerator.CAT_COMMUNICATION, CalcGenerator.CAT_TOUT};

    private TextView tvQuestion, tvProgress, tvFeedback, tvMode;
    private LinearLayout optionsContainer;
    private Button btnNext;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvMode = findViewById(R.id.tvMode);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Toucher le titre permet de changer de catégorie en cours de session
        tvMode.setOnClickListener(v -> choisirCategorie());

        btnNext.setOnClickListener(v -> {
            if (!answered) return;
            showNext();
        });

        // Reprendre la dernière catégorie choisie ; dialogue seulement à la première utilisation
        String savedCat = getSharedPreferences("parametres", MODE_PRIVATE)
                .getString("calc_categorie", null);
        if (savedCat != null) {
            categorie = savedCat;
            for (int i = 0; i < CAT_VALUES.length; i++) {
                if (CAT_VALUES[i].equals(categorie)) tvMode.setText(CAT_LABELS[i] + "  ▾");
            }
            showNext();
        } else {
            choisirCategorie();
        }
    }

    /** L'utilisateur choisit le type de calculs qu'il veut travailler */
    private void choisirCategorie() {
        int checked = 5;
        for (int i = 0; i < CAT_VALUES.length; i++) if (CAT_VALUES[i].equals(categorie)) checked = i;
        new android.app.AlertDialog.Builder(this)
                .setTitle("🧮 Quel type de calculs ?")
                .setCancelable(false)
                .setSingleChoiceItems(CAT_LABELS, checked, null)
                .setPositiveButton("Commencer", (d, w) -> {
                    int pos = ((android.app.AlertDialog) d).getListView().getCheckedItemPosition();
                    categorie = CAT_VALUES[pos >= 0 ? pos : 5];
                    tvMode.setText(CAT_LABELS[pos >= 0 ? pos : 5] + "  ▾");
                    getSharedPreferences("parametres", MODE_PRIVATE)
                            .edit().putString("calc_categorie", categorie).apply();
                    showNext();
                })
                .show();
    }

    private void showNext() {
        current = CalcGenerator.next(categorie);
        tvQuestion.setText(current.q);
        int taux = repondues > 0 ? (correctes * 100) / repondues : 0;
        tvProgress.setText(repondues + " faites" + (repondues > 0 ? " — " + taux + "%" : ""));

        optionsContainer.removeAllViews();
        List<Integer> ordre = new ArrayList<>();
        for (int i = 0; i < current.options.length; i++) ordre.add(i);
        Collections.shuffle(ordre);
        for (int pos = 0; pos < ordre.size(); pos++) {
            int i = ordre.get(pos);
            Button btn = new Button(this);
            btn.setText((char) ('A' + pos) + ". " + current.options[i]);
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
        btnNext.setText("➡️ Question suivante (∞)");
        answered = false;
    }

    private void checkAnswer(int selected) {
        if (answered) return;
        answered = true;
        repondues++;
        boolean correct = selected == current.correct;
        if (correct) correctes++;

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            Button btn = (Button) optionsContainer.getChildAt(i);
            btn.setEnabled(false);
            int original = (int) btn.getTag();
            if (original == current.correct)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            else if (original == selected)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }

        tvFeedback.setText((correct ? "✅ Correct ! " : "❌ Incorrect. ") + current.explication);
        tvFeedback.setVisibility(View.VISIBLE);
        int taux = (correctes * 100) / repondues;
        tvProgress.setText(repondues + " faites — " + taux + "%");
        btnNext.setEnabled(true);

        new ProgressManager(this).recordQuestion(correct);
    }
}
