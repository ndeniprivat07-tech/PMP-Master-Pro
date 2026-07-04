package com.pmp.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 1);
        String mode = getIntent().getStringExtra("mode");

        TextView tvScore = findViewById(R.id.tvScore);
        TextView tvPercentage = findViewById(R.id.tvPercentage);
        TextView tvMessage = findViewById(R.id.tvMessage);
        TextView tvMode = findViewById(R.id.tvMode);

        int percentage = total > 0 ? (score * 100) / total : 0;
        tvScore.setText(score + "/" + total);
        tvPercentage.setText(percentage + "%");
        tvMode.setText("examen".equals(mode) ? "Examen Réel" : "Entraînement");

        String message;
        if (percentage >= 80) message = "Excellent ! Vous êtes prêt pour le PMP !";
        else if (percentage >= 61) message = "Bon travail, continuez à réviser !";
        else message = "Révisez davantage et réessayez !";

        // ===== Rapport par domaine ECO + diagnostic (mode examen) =====
        if ("examen".equals(mode)) {
            StringBuilder sb = new StringBuilder(message).append("\n\n📊 RAPPORT PAR DOMAINE (comme au vrai PMP)\n");
            String[] domains = {"people", "process", "business"};
            String[] labels = {"People (42%)", "Process (50%)", "Business Env. (8%)"};
            int minPct = 100;
            for (int i = 0; i < domains.length; i++) {
                int t = getIntent().getIntExtra(domains[i] + "_total", 0);
                int c = getIntent().getIntExtra(domains[i] + "_correct", 0);
                if (t == 0) continue;
                int pct = (c * 100) / t;
                if (pct < minPct) minPct = pct;
                String verdict = pct >= 75 ? "🟢 Above Target" : pct >= 65 ? "🟡 Target" : "🔴 Below Target";
                sb.append("\n").append(labels[i]).append(" : ").append(pct).append("% — ").append(verdict);
            }

            // Historique des examens blancs + diagnostic "prêt"
            android.content.SharedPreferences prefs = getSharedPreferences("examens_blancs", MODE_PRIVATE);
            try {
                org.json.JSONArray hist = new org.json.JSONArray(prefs.getString("history", "[]"));
                org.json.JSONObject entry = new org.json.JSONObject();
                entry.put("pct", percentage);
                entry.put("minDomain", minPct);
                entry.put("date", System.currentTimeMillis());
                hist.put(entry);
                prefs.edit().putString("history", hist.toString()).apply();

                int n = hist.length();
                if (n >= 3) {
                    boolean pret = true;
                    for (int i = n - 3; i < n; i++) {
                        org.json.JSONObject e = hist.getJSONObject(i);
                        if (e.getInt("pct") < 75 || e.getInt("minDomain") < 70) { pret = false; break; }
                    }
                    sb.append("\n\n🎯 DIAGNOSTIC : ").append(pret
                            ? "✅ PRÊT POUR L'EXAMEN ! 3 examens blancs consécutifs ≥ 75% sans domaine faible. Vous pouvez vous inscrire."
                            : "⏳ Pas encore prêt : visez 3 examens blancs consécutifs ≥ 75% avec chaque domaine ≥ 70%.");
                } else {
                    sb.append("\n\n🎯 DIAGNOSTIC : ").append(n).append("/3 examens blancs passés. Il en faut 3 consécutifs ≥ 75% pour être déclaré prêt.");
                }
            } catch (Exception ignored) {}
            message = sb.toString();
        }
        tvMessage.setText(message);

        Button btnRetry = findViewById(R.id.btnRetry);
        Button btnHome = findViewById(R.id.btnHome);

        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("mode", mode);
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
