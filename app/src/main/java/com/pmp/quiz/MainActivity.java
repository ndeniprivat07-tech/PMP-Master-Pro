package com.pmp.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.pmp.quiz.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvProgress, tvLevel, tvQuestionsAnswered, tvSuccessRate, tvStreak;
    private CardView cardTraining, cardExam, cardLearn, cardStats;
    private Button btnStats;

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
        btnStats = findViewById(R.id.btnStats);
    }

    private void loadStats() {
        int[] stats = dbHelper.getStats();
        int total = stats[0];
        int correct = stats[1];

        tvQuestionsAnswered.setText(String.valueOf(total));
        int successRate = total > 0 ? (correct * 100) / total : 0;
        tvSuccessRate.setText(successRate + "%");
        int progress = (total * 100) / 5000;
        tvProgress.setText(progress + "%");

        String level;
        if (total < 50) level = "Débutant";
        else if (total < 200) level = "Intermédiaire";
        else if (total < 500) level = "Avancé";
        else if (total < 1000) level = "Expert";
        else level = "Maître";
        tvLevel.setText(level);

        tvStreak.setText((total / 10) + " jours");
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

        cardLearn.setOnClickListener(v -> {
            Toast.makeText(this, "Mode Apprentissage - Flashcards", Toast.LENGTH_SHORT).show();
        });

        cardStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        btnStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }
}
