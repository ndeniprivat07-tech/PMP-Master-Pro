package com.pmp.quiz.learn;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Révision répétitive (répétition espacée, algorithme SM-2 simplifié).
 * Toute question ratée dans l'app entre en révision et revient
 * à intervalles croissants : 1 jour, 3 jours, 7 jours, 16 jours...
 * Une question réussie 4 fois de suite en révision est considérée maîtrisée.
 */
public class ReviewManager {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private final SharedPreferences prefs;

    public static class ReviewItem {
        public String q;
        public String[] options = new String[4];
        public int correct;
        public String explication;
        public int reps;          // réussites consécutives en révision
        public double ease;       // facteur de facilité SM-2
        public long due;          // prochaine échéance (timestamp)
        public String key;
    }

    public ReviewManager(Context ctx) {
        prefs = ctx.getSharedPreferences("revision", Context.MODE_PRIVATE);
    }

    private JSONObject loadBank() {
        try {
            return new JSONObject(prefs.getString("bank", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void saveBank(JSONObject bank) {
        prefs.edit().putString("bank", bank.toString()).apply();
    }

    private String keyOf(String question) {
        return String.valueOf(question.hashCode());
    }

    /** Question ratée (dans un quiz, examen ou révision) : entre en révision, à revoir demain. */
    public void addFailure(String q, String[] options, int correct, String explication) {
        try {
            JSONObject bank = loadBank();
            JSONObject item = new JSONObject();
            item.put("q", q);
            JSONArray o = new JSONArray();
            for (String opt : options) o.put(opt);
            item.put("o", o);
            item.put("c", correct);
            item.put("e", explication == null ? "" : explication);
            item.put("reps", 0);
            item.put("ease", 2.5);
            item.put("due", System.currentTimeMillis() + DAY_MS); // à revoir demain
            bank.put(keyOf(q), item);
            saveBank(bank);
        } catch (Exception ignored) {}
    }

    /** Question réussie en révision : l'intervalle s'allonge (SM-2). Maîtrisée après 4 succès. */
    public void recordReviewSuccess(String key) {
        try {
            JSONObject bank = loadBank();
            if (!bank.has(key)) return;
            JSONObject item = bank.getJSONObject(key);
            int reps = item.getInt("reps") + 1;
            double ease = Math.max(1.3, item.getDouble("ease") + 0.1);
            if (reps >= 4) {
                bank.remove(key); // maîtrisée : sort de la révision
            } else {
                int intervalJours = reps == 1 ? 3 : reps == 2 ? 7 : 16;
                intervalJours = (int) Math.round(intervalJours * (ease / 2.5));
                item.put("reps", reps);
                item.put("ease", ease);
                item.put("due", System.currentTimeMillis() + intervalJours * DAY_MS);
                bank.put(key, item);
            }
            saveBank(bank);
        } catch (Exception ignored) {}
    }

    /** Question ratée en révision : on repart de zéro, à revoir demain. */
    public void recordReviewFailure(String key) {
        try {
            JSONObject bank = loadBank();
            if (!bank.has(key)) return;
            JSONObject item = bank.getJSONObject(key);
            item.put("reps", 0);
            item.put("ease", Math.max(1.3, item.getDouble("ease") - 0.2));
            item.put("due", System.currentTimeMillis() + DAY_MS);
            bank.put(key, item);
            saveBank(bank);
        } catch (Exception ignored) {}
    }

    /** Questions dont l'échéance est passée : à réviser MAINTENANT. */
    public List<ReviewItem> getDueItems() {
        List<ReviewItem> due = new ArrayList<>();
        long now = System.currentTimeMillis();
        JSONObject bank = loadBank();
        Iterator<String> keys = bank.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONObject item = bank.getJSONObject(key);
                if (item.getLong("due") <= now) {
                    due.add(toItem(key, item));
                }
            } catch (Exception ignored) {}
        }
        Collections.shuffle(due);
        return due;
    }

    public int getDueCount() {
        return getDueItems().size();
    }

    public int getTotalCount() {
        return loadBank().length();
    }

    private ReviewItem toItem(String key, JSONObject o) throws Exception {
        ReviewItem r = new ReviewItem();
        r.key = key;
        r.q = o.getString("q");
        JSONArray arr = o.getJSONArray("o");
        for (int i = 0; i < 4 && i < arr.length(); i++) r.options[i] = arr.getString(i);
        r.correct = o.getInt("c");
        r.explication = o.optString("e", "");
        r.reps = o.getInt("reps");
        r.ease = o.getDouble("ease");
        r.due = o.getLong("due");
        return r;
    }
}
