package com.pmp.quiz.database;

public class Question {
    private int id;
    private String domaine;
    private String niveau;
    private String chapitre;
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private int correctIndex;
    private String explication;
    private String reference;
    private String hint;
    private int tempsEstime;
    private int difficulte;

    public Question(int id, String domaine, String niveau, String chapitre,
                    String question, String option1, String option2,
                    String option3, String option4, int correctIndex,
                    String explication, String reference, String hint,
                    int tempsEstime, int difficulte) {
        this.id = id;
        this.domaine = domaine;
        this.niveau = niveau;
        this.chapitre = chapitre;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.correctIndex = correctIndex;
        this.explication = explication;
        this.reference = reference;
        this.hint = hint;
        this.tempsEstime = tempsEstime;
        this.difficulte = difficulte;
    }

    public int getId() { return id; }
    public String getDomaine() { return domaine; }
    public String getNiveau() { return niveau; }
    public String getChapitre() { return chapitre; }
    public String getQuestion() { return question; }
    public String getOption1() { return option1; }
    public String getOption2() { return option2; }
    public String getOption3() { return option3; }
    public String getOption4() { return option4; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplication() { return explication; }
    public String getReference() { return reference; }
    public String getHint() { return hint; }
    public int getTempsEstime() { return tempsEstime; }
    public int getDifficulte() { return difficulte; }

    public String[] getOptions() {
        return new String[]{option1, option2, option3, option4};
    }
}
