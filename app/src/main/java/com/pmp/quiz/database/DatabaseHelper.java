package com.pmp.quiz.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pmp_master_pro.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_QUESTIONS = "questions";
    private static final String COL_ID = "id";
    private static final String COL_DOMAINE = "domaine";
    private static final String COL_NIVEAU = "niveau";
    private static final String COL_CHAPITRE = "chapitre";
    private static final String COL_QUESTION = "question";
    private static final String COL_OPTION1 = "option1";
    private static final String COL_OPTION2 = "option2";
    private static final String COL_OPTION3 = "option3";
    private static final String COL_OPTION4 = "option4";
    private static final String COL_CORRECT = "correct_index";
    private static final String COL_EXPLICATION = "explication";
    private static final String COL_REFERENCE = "reference";
    private static final String COL_HINT = "hint";
    private static final String COL_TEMPS = "temps_estime";
    private static final String COL_DIFFICULTE = "difficulte";

    private static final String TABLE_STATS = "stats";
    private static final String TABLE_FLASHCARDS = "flashcards";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_QUESTIONS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_DOMAINE + " TEXT NOT NULL,"
                + COL_NIVEAU + " TEXT NOT NULL,"
                + COL_CHAPITRE + " TEXT,"
                + COL_QUESTION + " TEXT NOT NULL,"
                + COL_OPTION1 + " TEXT NOT NULL,"
                + COL_OPTION2 + " TEXT NOT NULL,"
                + COL_OPTION3 + " TEXT NOT NULL,"
                + COL_OPTION4 + " TEXT NOT NULL,"
                + COL_CORRECT + " INTEGER NOT NULL,"
                + COL_EXPLICATION + " TEXT,"
                + COL_REFERENCE + " TEXT,"
                + COL_HINT + " TEXT,"
                + COL_TEMPS + " INTEGER DEFAULT 60,"
                + COL_DIFFICULTE + " INTEGER DEFAULT 1"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_STATS + "("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + "total_questions INTEGER DEFAULT 0,"
                + "correct_answers INTEGER DEFAULT 0,"
                + "wrong_answers INTEGER DEFAULT 0,"
                + "temps_total INTEGER DEFAULT 0,"
                + "meilleur_score INTEGER DEFAULT 0"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_FLASHCARDS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "question_id INTEGER,"
                + "repetitions INTEGER DEFAULT 0,"
                + "interval_jours INTEGER DEFAULT 1,"
                + "ease_factor REAL DEFAULT 2.5,"
                + "derniere_revision INTEGER DEFAULT 0,"
                + "maitrise INTEGER DEFAULT 0"
                + ")");

        ContentValues statsValues = new ContentValues();
        statsValues.put(COL_ID, 1);
        statsValues.put("total_questions", 0);
        statsValues.put("correct_answers", 0);
        statsValues.put("wrong_answers", 0);
        statsValues.put("temps_total", 0);
        statsValues.put("meilleur_score", 0);
        db.insert(TABLE_STATS, null, statsValues);

        insertSampleQuestions(db);
    }

    private void insertSampleQuestions(SQLiteDatabase db) {
        addQ(db, "Valeur", "debutant", "Introduction",
            "Qu'est-ce qu'un projet selon le PMBOK ?",
            "Un effort temporaire pour créer un produit unique",
            "Une opération continue",
            "Un processus répétitif",
            "Une activité sans fin",
            0, "Un projet est temporaire et unique.", "PMBOK 7e p. 4", "Pensez à temporaire et unique", 30, 1);

        addQ(db, "Valeur", "debutant", "Introduction",
            "Qui sont les parties prenantes d'un projet ?",
            "Seulement l'équipe projet",
            "Toute personne affectée par le projet",
            "Seulement le client",
            "Seulement le sponsor",
            1, "Les parties prenantes incluent tous ceux qui sont affectés par le projet.", "PMBOK 7e p. 52", "Pensez à impact", 30, 1);

        addQ(db, "Valeur", "intermediaire", "Bénéfices",
            "Qu'est-ce que la valeur commerciale dans un projet ?",
            "Le profit généré uniquement",
            "L'ensemble des avantages tangibles et intangibles",
            "Le coût du projet",
            "La durée du projet",
            1, "La valeur commerciale inclut tous les avantages, pas seulement financiers.", "PMBOK 7e p. 45", "Pensez à tout ce qui apporte de la valeur", 45, 2);

        addQ(db, "Leadership", "intermediaire", "Leadership d'équipe",
            "Quelle est la différence entre un leader et un manager ?",
            "Un leader gère les tâches, un manager inspire",
            "Un leader inspire et motive, un manager planifie et organise",
            "Il n'y a pas de différence",
            "Un manager est plus important",
            1, "Le leadership inspire, le management organise.", "PMBOK 7e p. 73", "Pensez à inspiration vs organisation", 45, 3);

        addQ(db, "Leadership", "avance", "Motivation",
            "Selon le PMBOK 7e, le leadership du chef de projet doit être :",
            "Autoritaire et décisionnel",
            "Servant et adaptable",
            "Uniquement technique",
            "Basé sur la hiérarchie",
            1, "Le leadership servant est clé pour motiver les équipes et s'adapter.", "PMBOK 7e p. 81", "Pensez à servant leadership", 60, 3);

        addQ(db, "Qualité", "debutant", "Contrôle qualité",
            "La qualité dans le PMBOK 7e est abordée sous l'angle :",
            "Du contrôle qualité uniquement",
            "De l'assurance qualité",
            "De la culture de la qualité et de l'amélioration continue",
            "Des normes ISO exclusivement",
            2, "La qualité est vue comme une culture d'amélioration continue.", "PMBOK 7e p. 112", "Pensez à culture et amélioration", 30, 1);

        addQ(db, "Risques", "avance", "Identification",
            "Qu'est-ce que la gestion des risques dans un projet agile ?",
            "Gérer les risques de manière continue",
            "Faire une analyse de risques en début de projet",
            "Ignorer les risques",
            "Transférer tous les risques au client",
            0, "En agile, la gestion des risques est continue et itérative.", "PMBOK 7e p. 156", "Pensez à itérative", 60, 4);

        addQ(db, "Risques", "expert", "Analyse",
            "Comment le PMBOK 7e aborde-t-il la gestion des risques ?",
            "Uniquement les risques négatifs",
            "Risques et opportunités (incertitudes)",
            "Uniquement les risques financiers",
            "Pas de gestion des risques",
            1, "Le PMBOK 7e traite les incertitudes comme des risques ET des opportunités.", "PMBOK 7e p. 148", "Pensez à opportunités", 60, 4);

        addQ(db, "Performance", "intermediaire", "Suivi",
            "Quel domaine de performance traite de l'alignement du projet sur la stratégie ?",
            "Domaine Équipe",
            "Domaine Parties prenantes",
            "Domaine Planification",
            "Domaine Valeur",
            3, "Le domaine Valeur assure que le projet apporte les bénéfices stratégiques.", "PMBOK 7e p. 95", "Pensez à stratégique", 45, 2);

        addQ(db, "Parties prenantes", "debutant", "Engagement",
            "Que signifie engagement continu des parties prenantes ?",
            "Les informer une fois par mois",
            "Les impliquer de manière itérative tout au long du projet",
            "Les ignorer pour éviter les conflits",
            "Ne les consulter qu'en fin de projet",
            1, "L'engagement continu permet d'ajuster en permanence les attentes et la valeur.", "PMBOK 7e p. 60", "Pensez à continu", 30, 2);

        addQ(db, "Adaptabilité", "intermediaire", "Agilité",
            "Quelle approche est fortement encouragée dans le PMBOK 7e ?",
            "Approche prédictive uniquement",
            "Approche adaptative et hybride",
            "Approche agile uniquement",
            "Approche waterfall classique",
            1, "Le PMBOK 7e encourage l'adaptabilité et les approches hybrides selon le contexte.", "PMBOK 7e p. 28", "Pensez à adaptabilité", 45, 2);
    }

    private void addQ(SQLiteDatabase db, String domaine, String niveau, String chapitre,
                      String question, String opt1, String opt2, String opt3, String opt4,
                      int correct, String explication, String reference, String hint,
                      int temps, int difficulte) {
        ContentValues v = new ContentValues();
        v.put(COL_DOMAINE, domaine);
        v.put(COL_NIVEAU, niveau);
        v.put(COL_CHAPITRE, chapitre);
        v.put(COL_QUESTION, question);
        v.put(COL_OPTION1, opt1);
        v.put(COL_OPTION2, opt2);
        v.put(COL_OPTION3, opt3);
        v.put(COL_OPTION4, opt4);
        v.put(COL_CORRECT, correct);
        v.put(COL_EXPLICATION, explication);
        v.put(COL_REFERENCE, reference);
        v.put(COL_HINT, hint);
        v.put(COL_TEMPS, temps);
        v.put(COL_DIFFICULTE, difficulte);
        db.insert(TABLE_QUESTIONS, null, v);
    }

    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_QUESTIONS, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do { list.add(fromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Question> getQuestionsByLevel(String niveau) {
        List<Question> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_QUESTIONS, null, COL_NIVEAU + "=?", new String[]{niveau}, null, null, null);
        if (c.moveToFirst()) {
            do { list.add(fromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Question> getQuestionsByDomaine(String domaine) {
        List<Question> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_QUESTIONS, null, COL_DOMAINE + "=?", new String[]{domaine}, null, null, null);
        if (c.moveToFirst()) {
            do { list.add(fromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private Question fromCursor(Cursor c) {
        return new Question(
            c.getInt(c.getColumnIndexOrThrow(COL_ID)),
            c.getString(c.getColumnIndexOrThrow(COL_DOMAINE)),
            c.getString(c.getColumnIndexOrThrow(COL_NIVEAU)),
            c.getString(c.getColumnIndexOrThrow(COL_CHAPITRE)),
            c.getString(c.getColumnIndexOrThrow(COL_QUESTION)),
            c.getString(c.getColumnIndexOrThrow(COL_OPTION1)),
            c.getString(c.getColumnIndexOrThrow(COL_OPTION2)),
            c.getString(c.getColumnIndexOrThrow(COL_OPTION3)),
            c.getString(c.getColumnIndexOrThrow(COL_OPTION4)),
            c.getInt(c.getColumnIndexOrThrow(COL_CORRECT)),
            c.getString(c.getColumnIndexOrThrow(COL_EXPLICATION)),
            c.getString(c.getColumnIndexOrThrow(COL_REFERENCE)),
            c.getString(c.getColumnIndexOrThrow(COL_HINT)),
            c.getInt(c.getColumnIndexOrThrow(COL_TEMPS)),
            c.getInt(c.getColumnIndexOrThrow(COL_DIFFICULTE))
        );
    }

    public int[] getStats() {
        int[] stats = new int[5];
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_STATS, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            stats[0] = c.getInt(c.getColumnIndexOrThrow("total_questions"));
            stats[1] = c.getInt(c.getColumnIndexOrThrow("correct_answers"));
            stats[2] = c.getInt(c.getColumnIndexOrThrow("wrong_answers"));
            stats[3] = c.getInt(c.getColumnIndexOrThrow("temps_total"));
            stats[4] = c.getInt(c.getColumnIndexOrThrow("meilleur_score"));
        }
        c.close();
        return stats;
    }

    public void updateStats(boolean isCorrect, int temps) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_STATS, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            int total = c.getInt(c.getColumnIndexOrThrow("total_questions")) + 1;
            int correct = c.getInt(c.getColumnIndexOrThrow("correct_answers"));
            int wrong = c.getInt(c.getColumnIndexOrThrow("wrong_answers"));
            int tempsTotal = c.getInt(c.getColumnIndexOrThrow("temps_total")) + temps;
            if (isCorrect) correct++; else wrong++;
            ContentValues v = new ContentValues();
            v.put("total_questions", total);
            v.put("correct_answers", correct);
            v.put("wrong_answers", wrong);
            v.put("temps_total", tempsTotal);
            db.update(TABLE_STATS, v, COL_ID + "=1", null);
        }
        c.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FLASHCARDS);
        onCreate(db);
    }
}
