package com.pmp.quiz;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.pmp.quiz.learn.ProgressManager;
import com.pmp.quiz.learn.ReviewManager;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadStats();
    }

    private void loadStats() {
        ProgressManager pm = new ProgressManager(this);
        ReviewManager rm = new ReviewManager(this);

        int total = pm.getTotalQuestions();
        int correct = pm.getTotalCorrect();
        int wrong = total - correct;

        ((TextView) findViewById(R.id.tvTotal)).setText(String.valueOf(total));
        ((TextView) findViewById(R.id.tvCorrect)).setText(String.valueOf(correct));
        ((TextView) findViewById(R.id.tvWrong)).setText(String.valueOf(wrong));
        int percentage = total > 0 ? (correct * 100) / total : 0;
        ((TextView) findViewById(R.id.tvPercentage)).setText(percentage + "%");

        int streak = pm.getStreak();
        ((TextView) findViewById(R.id.tvTempsTotal)).setText(streak + (streak > 1 ? " jours" : " jour"));
        ((TextView) findViewById(R.id.tvMeilleurScore)).setText(String.valueOf(rm.getTotalCount()));

        // ===== Historique des examens blancs + verdict =====
        TextView tvHistory = findViewById(R.id.tvExamHistory);
        StringBuilder sb = new StringBuilder("📝 EXAMENS BLANCS\n");
        try {
            org.json.JSONArray hist = new org.json.JSONArray(
                    getSharedPreferences("examens_blancs", MODE_PRIVATE).getString("history", "[]"));
            if (hist.length() == 0) {
                sb.append("\nAucun examen blanc passé pour l'instant.\nLancez un « Examen Réel » (180 questions, 230 min) pour vous tester en conditions réelles.");
            } else {
                SimpleDateFormat df = new SimpleDateFormat("dd/MM", Locale.FRANCE);
                for (int i = Math.max(0, hist.length() - 8); i < hist.length(); i++) {
                    org.json.JSONObject e = hist.getJSONObject(i);
                    int pct = e.getInt("pct");
                    sb.append("\n• ").append(df.format(e.getLong("date")))
                            .append("  —  ").append(pct).append("%  ")
                            .append(pct >= 75 ? "🟢" : pct >= 65 ? "🟡" : "🔴");
                }

                // Verdict "Prêt pour l'examen"
                sb.append("\n\n🎯 DIAGNOSTIC : ");
                int n = hist.length();
                if (n < 3) {
                    sb.append(n).append("/3 examens blancs passés — il en faut 3 consécutifs ≥ 75% (chaque domaine ≥ 70%).");
                } else {
                    boolean pret = true;
                    for (int i = n - 3; i < n; i++) {
                        org.json.JSONObject e = hist.getJSONObject(i);
                        if (e.getInt("pct") < 75 || e.getInt("minDomain") < 70) { pret = false; break; }
                    }
                    sb.append(pret
                            ? "✅ PRÊT POUR L'EXAMEN ! 3 examens blancs consécutifs réussis. Vous pouvez vous inscrire."
                            : "⏳ Pas encore prêt — visez 3 examens blancs consécutifs ≥ 75% sans domaine faible.");
                }
            }
        } catch (Exception e) {
            sb.append("\nHistorique indisponible.");
        }

        sb.append("\n\n💡 Rappel : l'inscription au vrai examen PMP exige aussi 35 heures de formation certifiée (organisme agréé PMI) — cette app prépare au contenu mais ne délivre pas ces heures.");
        tvHistory.setText(sb.toString());
    }
}
