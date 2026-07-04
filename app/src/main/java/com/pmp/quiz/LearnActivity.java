package com.pmp.quiz;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import com.pmp.quiz.learn.ContentManager;
import com.pmp.quiz.learn.ProgressManager;
import java.util.List;

/** Chemin d'apprentissage type Duolingo : niveaux -> sous-niveaux verrouillés/débloqués/validés */
public class LearnActivity extends AppCompatActivity {

    private ProgressManager progress;
    private LinearLayout pathContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);
        progress = new ProgressManager(this);
        pathContainer = findViewById(R.id.pathContainer);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildPath();
    }

    private void buildPath() {
        pathContainer.removeAllViews();
        List<ContentManager.Niveau> niveaux = ContentManager.getNiveaux(this);

        TextView tvStreak = findViewById(R.id.tvStreakLearn);
        int streak = progress.getStreak();
        tvStreak.setText("🔥 " + streak + (streak > 1 ? " jours" : " jour"));

        for (int n = 0; n < niveaux.size(); n++) {
            ContentManager.Niveau niveau = niveaux.get(n);
            boolean niveauAccessible = (n == 0) || progress.isNiveauValide(niveaux.get(n - 1).id);

            // Titre du niveau
            TextView header = new TextView(this);
            header.setText(niveau.emoji + " NIVEAU " + niveau.id + " — " + niveau.titre
                    + (niveauAccessible ? "" : "  🔒"));
            header.setTextSize(18);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setTextColor(ContextCompat.getColor(this,
                    niveauAccessible ? R.color.primary : R.color.secondary));
            header.setPadding(8, 40, 8, 16);
            pathContainer.addView(header);

            // Sous-niveaux
            for (ContentManager.SousNiveau sub : niveau.sousNiveaux) {
                boolean valide = progress.isSousNiveauValide(sub.id);
                boolean debloque = progress.isSousNiveauDebloque(niveaux, sub.id);
                pathContainer.addView(makeCard(
                        stateIcon(valide, debloque) + "  " + sub.id + " — " + sub.titre,
                        valide ? "Validé ✓ (score " + progress.getScore(sub.id) + "%)"
                               : debloque ? subtitle(sub) : "Terminez l'étape précédente",
                        debloque,
                        valide,
                        v -> {
                            Intent i = new Intent(this, LessonActivity.class);
                            i.putExtra("subId", sub.id);
                            startActivity(i);
                        }));
            }

            // Examen de passage du niveau
            boolean examDebloque = niveauAccessible && progress.isExamenNiveauDebloque(niveau);
            boolean niveauValide = progress.isNiveauValide(niveau.id);
            String examKey = "niveau_" + niveau.id;
            String examSubtitle;
            if (niveauValide) examSubtitle = "Niveau validé ✓";
            else if (!examDebloque) examSubtitle = "Validez tous les sous-niveaux d'abord";
            else if (progress.isLocked(examKey))
                examSubtitle = "🔒 Réessayez dans " + progress.getRemainingLockText(examKey);
            else examSubtitle = "Seuil de réussite : 80%";

            final boolean canClick = examDebloque && !niveauValide && !progress.isLocked(examKey);
            pathContainer.addView(makeCard(
                    (niveauValide ? "✅" : examDebloque ? "🎓" : "🔒") + "  EXAMEN DE PASSAGE — Niveau " + niveau.id,
                    examSubtitle,
                    canClick,
                    niveauValide,
                    v -> {
                        Intent i = new Intent(this, ExamActivity.class);
                        i.putExtra("type", "examen_niveau");
                        i.putExtra("niveauId", niveau.id);
                        startActivity(i);
                    }));
        }

        TextView footer = new TextView(this);
        footer.setText("D'autres niveaux arrivent bientôt : Domaines PMBOK 7, PMP Mindset, Examens blancs 👑");
        footer.setTextSize(13);
        footer.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(16, 40, 16, 40);
        pathContainer.addView(footer);
    }

    private String subtitle(ContentManager.SousNiveau sub) {
        ProgressManager p = progress;
        if (!p.isLeconLue(sub.id)) return "📖 Leçon à lire";
        if (p.needsRemediation(sub.id) && !p.isComprehensionOk(sub.id))
            return "📚 Remédiation : relire + test de compréhension";
        if (p.isLocked("sub_" + sub.id))
            return "🔒 Examen dans " + p.getRemainingLockText("sub_" + sub.id);
        return "🎯 Examen disponible (80% requis)";
    }

    private String stateIcon(boolean valide, boolean debloque) {
        if (valide) return "✅";
        if (debloque) return "🔵";
        return "🔒";
    }

    private MaterialCardView makeCard(String title, String sub, boolean enabled,
                                      boolean valide, View.OnClickListener onClick) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(8, 8, 8, 8);
        card.setLayoutParams(lp);
        card.setRadius(24);
        card.setCardElevation(enabled ? 6 : 1);
        if (valide) card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_ok));
        else if (!enabled) card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_locked));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(36, 28, 36, 28);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this,
                enabled || valide ? R.color.primary : R.color.secondary));
        inner.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(sub);
        tvSub.setTextSize(13);
        tvSub.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        tvSub.setPadding(0, 6, 0, 0);
        inner.addView(tvSub);

        card.addView(inner);
        if (enabled || valide) {
            card.setClickable(true);
            card.setOnClickListener(onClick);
        }
        return card;
    }
}
