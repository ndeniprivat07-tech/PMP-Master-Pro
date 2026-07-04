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

        loadQuestions();

        if ("examen".equals(mode)) {
            startTimer(230 * 60);
            tvTimer.setVisibility(View.VISIBLE);
            tvLevel.setText("Examen Réel PMP");
        } else {
            tvLevel.setText("Entraînement");
        }

        showQuestion();
        setupClickListeners();
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
        questions = all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
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
        return "process";
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishQuiz();
            return;
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

    private void startTimer(int seconds) {
        timer = new CountDownTimer((long) seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long t = millisUntilFinished / 1000;
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
