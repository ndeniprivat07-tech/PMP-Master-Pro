package com.pmp.quiz.learn;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.List;

/**
 * Gère la progression type Duolingo :
 * - déblocage linéaire des sous-niveaux et niveaux
 * - verrouillage 24h après échec à un examen
 * - remédiation : relire la leçon + test de compréhension avant nouvelle tentative
 * - streak (jours consécutifs d'étude)
 */
public class ProgressManager {

    public static final long LOCK_DURATION_MS = 24L * 60 * 60 * 1000; // 24 heures
    public static final int SEUIL_REUSSITE = 80; // 80%

    private final SharedPreferences prefs;

    public ProgressManager(Context ctx) {
        prefs = ctx.getSharedPreferences("progression", Context.MODE_PRIVATE);
    }

    // ===== Leçons =====

    public boolean isLeconLue(String subId) {
        return prefs.getBoolean("lecon_" + subId, false);
    }

    public void setLeconLue(String subId) {
        prefs.edit().putBoolean("lecon_" + subId, true).apply();
        recordStudy();
    }

    // ===== Sous-niveaux =====

    public boolean isSousNiveauValide(String subId) {
        return prefs.getBoolean("valide_" + subId, false);
    }

    public void setSousNiveauValide(String subId, int score) {
        prefs.edit()
                .putBoolean("valide_" + subId, true)
                .putInt("score_" + subId, score)
                .putBoolean("remediation_" + subId, false)
                .apply();
        recordStudy();
    }

    public int getScore(String subId) {
        return prefs.getInt("score_" + subId, -1);
    }

    // ===== Niveaux =====

    public boolean isNiveauValide(int niveauId) {
        return prefs.getBoolean("niveau_valide_" + niveauId, false);
    }

    public void setNiveauValide(int niveauId, int score) {
        prefs.edit()
                .putBoolean("niveau_valide_" + niveauId, true)
                .putInt("niveau_score_" + niveauId, score)
                .apply();
        recordStudy();
    }

    // ===== Verrouillage 24h =====

    public long getLockUntil(String examKey) {
        return prefs.getLong("lock_" + examKey, 0);
    }

    public boolean isLocked(String examKey) {
        return System.currentTimeMillis() < getLockUntil(examKey);
    }

    public void lockExam(String examKey) {
        prefs.edit().putLong("lock_" + examKey, System.currentTimeMillis() + LOCK_DURATION_MS).apply();
    }

    public String getRemainingLockText(String examKey) {
        long remaining = getLockUntil(examKey) - System.currentTimeMillis();
        if (remaining <= 0) return "";
        long h = remaining / 3600000;
        long m = (remaining % 3600000) / 60000;
        return h + "h " + m + "min";
    }

    // ===== Remédiation (après échec) =====

    public boolean needsRemediation(String subId) {
        return prefs.getBoolean("remediation_" + subId, false);
    }

    public void setRemediation(String subId) {
        prefs.edit()
                .putBoolean("remediation_" + subId, true)
                .putBoolean("comprehension_" + subId, false)
                .apply();
    }

    public boolean isComprehensionOk(String subId) {
        return prefs.getBoolean("comprehension_" + subId, false);
    }

    public void setComprehensionOk(String subId) {
        prefs.edit().putBoolean("comprehension_" + subId, true).apply();
        recordStudy();
    }

    // ===== Logique de déblocage =====

    /** Le sous-niveau est-il accessible ? (progression linéaire stricte) */
    public boolean isSousNiveauDebloque(List<ContentManager.Niveau> niveaux, String subId) {
        for (int n = 0; n < niveaux.size(); n++) {
            ContentManager.Niveau niveau = niveaux.get(n);
            // Le niveau lui-même doit être débloqué (niveau précédent validé)
            if (n > 0 && !isNiveauValide(niveaux.get(n - 1).id)) {
                for (ContentManager.SousNiveau s : niveau.sousNiveaux) {
                    if (s.id.equals(subId)) return false;
                }
                continue;
            }
            for (int i = 0; i < niveau.sousNiveaux.size(); i++) {
                if (niveau.sousNiveaux.get(i).id.equals(subId)) {
                    if (i == 0) return true; // premier sous-niveau du niveau
                    return isSousNiveauValide(niveau.sousNiveaux.get(i - 1).id);
                }
            }
        }
        return false;
    }

    /** L'examen de passage du niveau est-il accessible ? (tous les sous-niveaux validés) */
    public boolean isExamenNiveauDebloque(ContentManager.Niveau niveau) {
        for (ContentManager.SousNiveau s : niveau.sousNiveaux) {
            if (!isSousNiveauValide(s.id)) return false;
        }
        return true;
    }

    /**
     * Peut-on passer l'examen du sous-niveau ?
     * Conditions : leçon lue + pas de verrou 24h actif + (pas de remédiation OU compréhension validée)
     */
    public boolean canTakeExamen(String subId) {
        if (!isLeconLue(subId)) return false;
        if (isLocked("sub_" + subId)) return false;
        if (needsRemediation(subId) && !isComprehensionOk(subId)) return false;
        return true;
    }

    /** Message expliquant pourquoi l'examen est bloqué */
    public String getExamenBlockReason(String subId) {
        if (!isLeconLue(subId)) return "📖 Lisez d'abord la leçon";
        boolean locked = isLocked("sub_" + subId);
        boolean remed = needsRemediation(subId) && !isComprehensionOk(subId);
        if (locked && remed) return "🔒 Réessayez dans " + getRemainingLockText("sub_" + subId)
                + " — relisez la leçon et validez le test de compréhension";
        if (locked) return "🔒 Réessayez dans " + getRemainingLockText("sub_" + subId);
        if (remed) return "📚 Relisez la leçon puis validez le test de compréhension";
        return "";
    }

    // ===== Streak (jours consécutifs) =====

    private int dayNumber() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    public void recordStudy() {
        int today = dayNumber();
        int lastDay = prefs.getInt("streak_last_day", 0);
        int streak = prefs.getInt("streak_count", 0);
        if (lastDay == today) return; // déjà compté aujourd'hui

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        int yesterdayNum = yesterday.get(Calendar.YEAR) * 1000 + yesterday.get(Calendar.DAY_OF_YEAR);

        if (lastDay == yesterdayNum) streak++;
        else streak = 1;

        prefs.edit().putInt("streak_last_day", today).putInt("streak_count", streak).apply();
    }

    // ===== Compteur de questions (toutes activités confondues) =====

    /** Enregistre une question répondue — alimente l'objectif quotidien et les statistiques globales */
    public void recordQuestion(boolean correct) {
        recordStudy();
        int today = dayNumber();
        prefs.edit()
                .putInt("q_day_" + today, prefs.getInt("q_day_" + today, 0) + 1)
                .putInt("q_total", prefs.getInt("q_total", 0) + 1)
                .putInt("q_correct", prefs.getInt("q_correct", 0) + (correct ? 1 : 0))
                .apply();
    }

    public int getTodayCount() {
        return prefs.getInt("q_day_" + dayNumber(), 0);
    }

    public int getTotalQuestions() {
        return prefs.getInt("q_total", 0);
    }

    public int getTotalCorrect() {
        return prefs.getInt("q_correct", 0);
    }

    public int getStreak() {
        int today = dayNumber();
        int lastDay = prefs.getInt("streak_last_day", 0);
        int streak = prefs.getInt("streak_count", 0);
        if (lastDay == today) return streak;
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        int yesterdayNum = yesterday.get(Calendar.YEAR) * 1000 + yesterday.get(Calendar.DAY_OF_YEAR);
        if (lastDay == yesterdayNum) return streak; // streak encore vivant, pas encore étudié aujourd'hui
        return 0; // streak cassé
    }
}
