package com.pmp.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.pmp.quiz.database.DatabaseHelper;
import com.pmp.quiz.database.Question;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    // Suivi par domaine ECO (People / Process / Business) pour le rapport d'examen
    private final java.util.HashMap<Integer, String> ecoMap = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> ecoTotal = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> ecoCorrect = new java.util.HashMap<>();
    private String mode;
    private CountDownTimer timer;
    private long timeLeft = 0; // secondes restantes du chrono d'examen
    private boolean isAnswerShown = false;
    private int hintsUsed = 0;
    private static final int MAX_HINTS = 3;

    private TextView tvQuestion, tvProgress, tvTimer, tvScore, tvLevel, tvFeedback;
    private LinearLayout optionsContainer;
    private Button btnNext, btnHint;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        dbHelper = new DatabaseHelper(this);
        initViews();

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "entrainement";

        setupClickListeners();

        // Reprise de session : un examen interrompu n'est pas perdu
        android.content.SharedPreferences sess = getSharedPreferences("quiz_session", MODE_PRIVATE);
        String saved = sess.getString("state", null);
        if (saved != null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("⏸️ Session interrompue trouvée")
                    .setMessage("Vous avez un quiz/examen en cours non terminé. Voulez-vous le reprendre là où vous vous étiez arrêté ?")
                    .setCancelable(false)
                    .setPositiveButton("▶️ Reprendre", (d, w) -> restoreSession(saved))
                    .setNegativeButton("🗑️ Recommencer", (d, w) -> {
                        clearSession();
                        startFresh();
                    })
                    .show();
        } else {
            startFresh();
        }
    }

    private void startFresh() {
        loadQuestions();

        if ("examen".equals(mode)) {
            startTimer(230 * 60);
            tvTimer.setVisibility(View.VISIBLE);
            tvLevel.setText("Examen Réel PMP");
        } else {
            tvLevel.setText("Entraînement");
        }

        showQuestion();
    }

    // ===== Sauvegarde / reprise de session =====

    private void saveSession() {
        try {
            org.json.JSONObject state = new org.json.JSONObject();
            state.put("mode", mode);
            state.put("index", currentIndex);
            state.put("score", score);
            state.put("timeLeft", timeLeft);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Question q : questions) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("q", q.getQuestion());
                org.json.JSONArray opts = new org.json.JSONArray();
                for (String s : q.getOptions()) opts.put(s);
                o.put("o", opts);
                o.put("c", q.getCorrectIndex());
                o.put("e", q.getExplication() == null ? "" : q.getExplication());
                String eco = ecoMap.get(q.getId());
                o.put("d", eco == null ? "process" : eco);
                arr.put(o);
            }
            state.put("questions", arr);
            org.json.JSONObject ecoC = new org.json.JSONObject();
            for (java.util.Map.Entry<String, Integer> e : ecoCorrect.entrySet()) ecoC.put(e.getKey(), e.getValue());
            state.put("ecoCorrect", ecoC);
            org.json.JSONObject ecoT = new org.json.JSONObject();
            for (java.util.Map.Entry<String, Integer> e : ecoTotal.entrySet()) ecoT.put(e.getKey(), e.getValue());
            state.put("ecoTotal", ecoT);
            getSharedPreferences("quiz_session", MODE_PRIVATE)
                    .edit().putString("state", state.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void restoreSession(String saved) {
        try {
            org.json.JSONObject state = new org.json.JSONObject(saved);
            mode = state.getString("mode");
            currentIndex = state.getInt("index");
            score = state.getInt("score");
            timeLeft = state.getLong("timeLeft");
            questions = new ArrayList<>();
            org.json.JSONArray arr = state.getJSONArray("questions");
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                org.json.JSONArray opts = o.getJSONArray("o");
                questions.add(new Question(300000 + i, "reprise", "reprise", "",
                        o.getString("q"), opts.getString(0), opts.getString(1),
                        opts.getString(2), opts.getString(3), o.getInt("c"),
                        o.getString("e"), "", "", 60, 2));
                ecoMap.put(300000 + i, o.optString("d", "process"));
            }
            org.json.JSONObject ecoC = state.getJSONObject("ecoCorrect");
            java.util.Iterator<String> it = ecoC.keys();
            while (it.hasNext()) { String k = it.next(); ecoCorrect.put(k, ecoC.getInt(k)); }
            org.json.JSONObject ecoT = state.getJSONObject("ecoTotal");
            it = ecoT.keys();
            while (it.hasNext()) { String k = it.next(); ecoTotal.put(k, ecoT.getInt(k)); }

            if ("examen".equals(mode)) {
                tvLevel.setText("Examen Réel PMP");
                if (timeLeft > 0) {
                    startTimer((int) timeLeft);
                    tvTimer.setVisibility(View.VISIBLE);
                }
                if (currentIndex > 60) pausesProposees.add(60);
                if (currentIndex > 120) pausesProposees.add(120);
            } else {
                tvLevel.setText("Entraînement");
            }
            showQuestion();
        } catch (Exception e) {
            clearSession();
            startFresh();
        }
    }

    private void clearSession() {
        getSharedPreferences("quiz_session", MODE_PRIVATE).edit().remove("state").apply();
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvFeedback = findViewById(R.id.tvFeedback);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnNext = findViewById(R.id.btnNext);
        btnHint = findViewById(R.id.btnHint);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadQuestions() {
        List<Question> all = dbHelper.getAllQuestions();
        for (Question q : all) ecoMap.put(q.getId(), ecoFromDomaine(q.getDomaine()));

        // Questions du parcours d'apprentissage (content.json)
        int syntheticId = 100000;
        for (com.pmp.quiz.learn.ContentManager.Niveau niveau :
                com.pmp.quiz.learn.ContentManager.getNiveaux(this)) {
            for (com.pmp.quiz.learn.ContentManager.SousNiveau sub : niveau.sousNiveaux) {
                List<com.pmp.quiz.learn.ContentManager.QItem> pool = new ArrayList<>(sub.pratique);
                pool.addAll(sub.examen);
                for (com.pmp.quiz.learn.ContentManager.QItem q : pool) {
                    ecoMap.put(syntheticId, ecoFromSubId(sub.id));
                    all.add(new Question(syntheticId++, niveau.titre, "parcours", sub.titre,
                            q.q, q.options[0], q.options[1], q.options[2], q.options[3],
                            q.correct, q.explication, sub.id + " — " + sub.titre, "", 60, 2));
                }
            }
        }

        // Banque de questions d'examen (étiquetées ECO)
        int bankId = 200000;
        for (com.pmp.quiz.learn.ContentManager.BankQ q :
                com.pmp.quiz.learn.ContentManager.getBank(this)) {
            ecoMap.put(bankId, q.domain);
            all.add(new Question(bankId++, "Banque examen", "banque", q.domain,
                    q.q, q.options[0], q.options[1], q.options[2], q.options[3],
                    q.correct, q.explication, "Banque PMP", "", 76, 3));
        }

        Collections.shuffle(all);
        int limit = "examen".equals(mode) ? 180 : 20;

        if ("examen".equals(mode)) {
            // Anti-répétition : privilégier les questions jamais vues dans les examens précédents
            java.util.Set<String> dejaVues = getSharedPreferences("examens_blancs", MODE_PRIVATE)
                    .getStringSet("questions_vues", new java.util.HashSet<>());
            List<Question> nouvelles = new ArrayList<>();
            List<Question> vues = new ArrayList<>();
            for (Question q : all) {
                if (dejaVues.contains(String.valueOf(q.getQuestion().hashCode()))) vues.add(q);
                else nouvelles.add(q);
            }
            List<Question> ordered = new ArrayList<>(nouvelles);
            ordered.addAll(vues); // les vues ne servent qu'en complément
            questions = ordered.size() > limit ? new ArrayList<>(ordered.subList(0, limit)) : ordered;
            Collections.shuffle(questions);
        } else {
            questions = all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
        }
    }

    /** Mémorise les questions utilisées dans cet examen blanc (anti-répétition) */
    private void marquerQuestionsVues() {
        android.content.SharedPreferences prefs = getSharedPreferences("examens_blancs", MODE_PRIVATE);
        java.util.Set<String> vues = new java.util.HashSet<>(
                prefs.getStringSet("questions_vues", new java.util.HashSet<>()));
        for (Question q : questions) vues.add(String.valueOf(q.getQuestion().hashCode()));
        prefs.edit().putStringSet("questions_vues", vues).apply();
    }

    /** Domaine ECO à partir du domaine d'une question de la base */
    private String ecoFromDomaine(String domaine) {
        if (domaine == null) return "process";
        String d = domaine.toLowerCase();
        if (d.contains("leadership") || d.contains("parties prenantes")) return "people";
        if (d.contains("valeur")) return "business";
        return "process";
    }

    /** Domaine ECO à partir du sous-niveau du parcours */
    private String ecoFromSubId(String subId) {
        if (subId.startsWith("1.2") || subId.startsWith("3.1") || subId.startsWith("3.2")
                || subId.startsWith("4.")) return "people";
        if (subId.startsWith("3.9")) return "business";
        return "process";
    }

    private final java.util.HashSet<Integer> pausesProposees = new java.util.HashSet<>();

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        // Pauses réalistes du vrai examen PMP : après Q60 et Q120 (chrono suspendu)
        if ("examen".equals(mode) && questions.size() >= 120
                && (currentIndex == 60 || currentIndex == 120)
                && !pausesProposees.contains(currentIndex)) {
            pausesProposees.add(currentIndex);
            proposerPause();
        }

        Question q = questions.get(currentIndex);
        tvQuestion.setText(q.getQuestion());

        int total = questions.size();
        tvProgress.setText((currentIndex + 1) + "/" + total);
        progressBar.setProgress((currentIndex + 1) * 100 / total);

        optionsContainer.removeAllViews();
        String[] options = q.getOptions();
        for (int i = 0; i < options.length; i++) {
            Button btn = new Button(this);
            btn.setText((char) ('A' + i) + ". " + options[i]);
            btn.setTag(i);
            btn.setOnClickListener(v -> checkAnswer((int) v.getTag()));
            btn.setPadding(24, 16, 24, 16);
            btn.setTextSize(16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);
            btn.setLayoutParams(params);
            optionsContainer.addView(btn);
        }

        tvFeedback.setVisibility(View.GONE);
        btnNext.setEnabled(false);
        btnHint.setEnabled(hintsUsed < MAX_HINTS);
        isAnswerShown = false;
        tvScore.setText("Score: " + score);
    }

    private void checkAnswer(int selected) {
        if (isAnswerShown) return;
        isAnswerShown = true;

        Question q = questions.get(currentIndex);
        boolean isCorrect = selected == q.getCorrectIndex();

        new com.pmp.quiz.learn.ProgressManager(this).recordQuestion(isCorrect);
        if (isCorrect) {
            score++;
        } else {
            // Répétition espacée : la question ratée entre en révision
            new com.pmp.quiz.learn.ReviewManager(this).addFailure(
                    q.getQuestion(), q.getOptions(), q.getCorrectIndex(), q.getExplication());
        }
        // Suivi par domaine ECO
        String eco = ecoMap.get(q.getId());
        if (eco == null) eco = "process";
        ecoTotal.put(eco, ecoTotal.containsKey(eco) ? ecoTotal.get(eco) + 1 : 1);
        if (isCorrect) ecoCorrect.put(eco, ecoCorrect.containsKey(eco) ? ecoCorrect.get(eco) + 1 : 1);
        dbHelper.updateStats(isCorrect, 30);

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            Button btn = (Button) optionsContainer.getChildAt(i);
            btn.setEnabled(false);
            if (i == q.getCorrectIndex()) {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            } else if (i == selected && !isCorrect) {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            }
        }

        String feedback = (isCorrect ? "Correct ! " : "Incorrect. ") + q.getExplication();
        if (q.getReference() != null && !q.getReference().isEmpty()) {
            feedback += "\nRef: " + q.getReference();
        }
        tvFeedback.setText(feedback);
        tvFeedback.setVisibility(View.VISIBLE);
        tvScore.setText("Score: " + score);
        btnNext.setEnabled(true);
    }

    /**
     * Pause de 10 minutes comme au vrai examen PMP (après Q60 et Q120).
     * Le chrono principal est suspendu ; on ne peut pas revenir aux questions précédentes.
     */
    private void proposerPause() {
        if (timer != null) timer.cancel(); // suspendre le chrono principal

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("☕ Pause officielle — 10 minutes")
                .setMessage("Comme au vrai examen PMP :\n\n• Pause de 10 minutes (chrono suspendu)\n• Vous ne pourrez PAS revenir aux questions précédentes\n• Levez-vous, hydratez-vous, respirez !\n\nTemps restant : 10:00")
                .setCancelable(false)
                .setPositiveButton("▶️ Reprendre l'examen", null)
                .create();
        dialog.show();

        final CountDownTimer pauseTimer = new CountDownTimer(10 * 60 * 1000, 1000) {
            public void onTick(long ms) {
                long m = ms / 60000, s = (ms % 60000) / 1000;
                dialog.setMessage("Comme au vrai examen PMP :\n\n• Pause de 10 minutes (chrono suspendu)\n• Vous ne pourrez PAS revenir aux questions précédentes\n• Levez-vous, hydratez-vous, respirez !\n\nTemps restant : "
                        + String.format("%02d:%02d", m, s));
            }
            public void onFinish() {
                if (dialog.isShowing()) dialog.dismiss();
                if (timeLeft > 0) startTimer((int) timeLeft); // reprendre le chrono
            }
        }.start();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            pauseTimer.cancel();
            dialog.dismiss();
            if (timeLeft > 0) startTimer((int) timeLeft); // reprendre le chrono
        });
    }

    private void startTimer(int seconds) {
        timer = new CountDownTimer((long) seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long t = millisUntilFinished / 1000;
                timeLeft = t; // mémorisé pour la reprise de session et les pauses
                tvTimer.setText(String.format("%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60));
            }
            public void onFinish() {
                tvTimer.setText("Temps écoule !");
                finishQuiz();
            }
        }.start();
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            if (!isAnswerShown) {
                Toast.makeText(this, "Répondez d'abord à la question", Toast.LENGTH_SHORT).show();
                return;
            }
            currentIndex++;
            saveSession(); // la progression est conservée en cas d'interruption
            showQuestion();
        });

        btnHint.setOnClickListener(v -> {
            if (hintsUsed < MAX_HINTS) {
                hintsUsed++;
                btnHint.setEnabled(hintsUsed < MAX_HINTS);
                Question q = questions.get(currentIndex);
                tvFeedback.setText("Indice : " + q.getHint());
                tvFeedback.setVisibility(View.VISIBLE);
            }
        });

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void finishQuiz() {
        clearSession();
        if ("examen".equals(mode)) marquerQuestionsVues();
        if (timer != null) timer.cancel();
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("score", score);
        intent.putExtra("total", questions.size());
        intent.putExtra("mode", mode);
        // Rapport par domaine ECO
        for (String d : new String[]{"people", "process", "business"}) {
            intent.putExtra(d + "_total", ecoTotal.containsKey(d) ? ecoTotal.get(d) : 0);
            intent.putExtra(d + "_correct", ecoCorrect.containsKey(d) ? ecoCorrect.get(d) : 0);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
