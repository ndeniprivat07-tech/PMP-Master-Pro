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
