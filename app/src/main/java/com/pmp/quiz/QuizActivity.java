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
        // Ajouter toutes les questions du parcours d'apprentissage (content.json)
        int syntheticId = 100000;
        for (com.pmp.quiz.learn.ContentManager.Niveau niveau :
                com.pmp.quiz.learn.ContentManager.getNiveaux(this)) {
            for (com.pmp.quiz.learn.ContentManager.SousNiveau sub : niveau.sousNiveaux) {
                List<com.pmp.quiz.learn.ContentManager.QItem> pool = new ArrayList<>(sub.pratique);
                pool.addAll(sub.examen);
                for (com.pmp.quiz.learn.ContentManager.QItem q : pool) {
                    all.add(new Question(syntheticId++, niveau.titre, "parcours", sub.titre,
                            q.q, q.options[0], q.options[1], q.options[2], q.options[3],
                            q.correct, q.explication, sub.id + " — " + sub.titre, "", 60, 2));
                }
            }
        }
        Collections.shuffle(all);
        int limit = "examen".equals(mode) ? 180 : 20;
        questions = all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
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

        if (isCorrect) score++;
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
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
