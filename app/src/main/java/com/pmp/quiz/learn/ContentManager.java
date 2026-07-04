package com.pmp.quiz.learn;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContentManager {

    public static class QItem {
        public String q;
        public String[] options;
        public int correct;
        public String explication;
    }

    public static class Section {
        public String titre;
        public String texte;
    }

    public static class SousNiveau {
        public String id;
        public String titre;
        public String lecon;
        public List<QItem> pratique = new ArrayList<>();
        public List<QItem> examen = new ArrayList<>();

        private List<Section> sections;

        /** Découpe la leçon en sections à partir des titres numérotés ("1. TITRE"). */
        public List<Section> getSections() {
            if (sections != null) return sections;
            sections = new ArrayList<>();
            String[] lines = lecon.split("\n");
            Section current = null;
            StringBuilder body = new StringBuilder();
            for (String line : lines) {
                if (line.matches("^\\d+\\.\\s+.*")) {
                    if (current != null) {
                        current.texte = body.toString().trim();
                        sections.add(current);
                    }
                    current = new Section();
                    current.titre = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                    body = new StringBuilder();
                } else {
                    body.append(line).append("\n");
                }
            }
            if (current != null) {
                current.texte = body.toString().trim();
                sections.add(current);
            }
            // Sécurité : si aucune section détectée, tout le texte devient une section unique
            if (sections.isEmpty()) {
                Section s = new Section();
                s.titre = titre;
                s.texte = lecon;
                sections.add(s);
            }
            return sections;
        }
    }

    public static class Niveau {
        public int id;
        public String titre;
        public String emoji;
        public List<SousNiveau> sousNiveaux = new ArrayList<>();
    }

    private static List<Niveau> cache;

    public static List<Niveau> getNiveaux(Context ctx) {
        if (cache != null) return cache;
        List<Niveau> niveaux = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    ctx.getAssets().open("content.json"), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.getJSONArray("niveaux");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject n = arr.getJSONObject(i);
                Niveau niveau = new Niveau();
                niveau.id = n.getInt("id");
                niveau.titre = n.getString("titre");
                niveau.emoji = n.optString("emoji", "");
                JSONArray subs = n.getJSONArray("sousNiveaux");
                for (int j = 0; j < subs.length(); j++) {
                    JSONObject s = subs.getJSONObject(j);
                    SousNiveau sub = new SousNiveau();
                    sub.id = s.getString("id");
                    sub.titre = s.getString("titre");
                    sub.lecon = s.getString("lecon");
                    sub.pratique = parseQuestions(s.getJSONArray("pratique"));
                    sub.examen = parseQuestions(s.getJSONArray("examen"));
                    niveau.sousNiveaux.add(sub);
                }
                niveaux.add(niveau);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cache = niveaux;
        return niveaux;
    }

    private static List<QItem> parseQuestions(JSONArray arr) throws Exception {
        List<QItem> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            QItem q = new QItem();
            q.q = o.getString("q");
            JSONArray opts = o.getJSONArray("o");
            q.options = new String[opts.length()];
            for (int k = 0; k < opts.length(); k++) q.options[k] = opts.getString(k);
            q.correct = o.getInt("c");
            q.explication = o.getString("e");
            list.add(q);
        }
        return list;
    }

    // ===== Banque de questions d'examen (étiquetées par domaine ECO) =====

    public static class BankQ extends QItem {
        public String domain; // people | process | business
    }

    private static List<BankQ> bankCache;

    public static List<BankQ> getBank(Context ctx) {
        if (bankCache != null) return bankCache;
        List<BankQ> bank = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    ctx.getAssets().open("exam_bank.json"), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.getJSONArray("questions");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                BankQ q = new BankQ();
                q.q = o.getString("q");
                JSONArray opts = o.getJSONArray("o");
                q.options = new String[opts.length()];
                for (int k = 0; k < opts.length(); k++) q.options[k] = opts.getString(k);
                q.correct = o.getInt("c");
                q.explication = o.getString("e");
                q.domain = o.optString("d", "process");
                bank.add(q);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        bankCache = bank;
        return bank;
    }

    public static SousNiveau findSousNiveau(Context ctx, String subId) {
        for (Niveau n : getNiveaux(ctx)) {
            for (SousNiveau s : n.sousNiveaux) {
                if (s.id.equals(subId)) return s;
            }
        }
        return null;
    }

    public static Niveau findNiveau(Context ctx, int niveauId) {
        for (Niveau n : getNiveaux(ctx)) {
            if (n.id == niveauId) return n;
        }
        return null;
    }
}
