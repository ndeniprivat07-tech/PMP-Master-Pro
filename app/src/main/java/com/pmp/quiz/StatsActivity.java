package com.pmp.quiz;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.pmp.quiz.database.DatabaseHelper;

public class StatsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvTotal, tvCorrect, tvWrong, tvPercentage, tvTempsTotal, tvMeilleurScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadStats();

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvTotal = findViewById(R.id.tvTotal);
        tvCorrect = findViewById(R.id.tvCorrect);
        tvWrong = findViewById(R.id.tvWrong);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvTempsTotal = findViewById(R.id.tvTempsTotal);
        tvMeilleurScore = findViewById(R.id.tvMeilleurScore);
    }

    private void loadStats() {
        int[] stats = dbHelper.getStats();
        int total = stats[0];
        int correct = stats[1];
        int wrong = stats[2];
        int tempsTotal = stats[3];
        int meilleurScore = stats[4];

        tvTotal.setText(String.valueOf(total));
        tvCorrect.setText(String.valueOf(correct));
        tvWrong.setText(String.valueOf(wrong));
        int percentage = total > 0 ? (correct * 100) / total : 0;
        tvPercentage.setText(percentage + "%");
        int heures = tempsTotal / 3600;
        int minutes = (tempsTotal % 3600) / 60;
        tvTempsTotal.setText(heures + "h " + minutes + "min");
        tvMeilleurScore.setText(String.valueOf(meilleurScore));
    }
}
