package com.pmp.quiz.models;

public class Flashcard {
    private int id;
    private int questionId;
    private int repetitions;
    private int intervalJours;
    private double easeFactor;
    private long derniereRevision;
    private boolean maitrise;
    private String question;
    private String reponse;

    public Flashcard(int id, int questionId, int repetitions, int intervalJours,
                     double easeFactor, long derniereRevision, boolean maitrise,
                     String question, String reponse) {
        this.id = id;
        this.questionId = questionId;
        this.repetitions = repetitions;
        this.intervalJours = intervalJours;
        this.easeFactor = easeFactor;
        this.derniereRevision = derniereRevision;
        this.maitrise = maitrise;
        this.question = question;
        this.reponse = reponse;
    }

    public int getId() { return id; }
    public int getQuestionId() { return questionId; }
    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }
    public int getIntervalJours() { return intervalJours; }
    public void setIntervalJours(int intervalJours) { this.intervalJours = intervalJours; }
    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }
    public long getDerniereRevision() { return derniereRevision; }
    public void setDerniereRevision(long derniereRevision) { this.derniereRevision = derniereRevision; }
    public boolean isMaitrise() { return maitrise; }
    public void setMaitrise(boolean maitrise) { this.maitrise = maitrise; }
    public String getQuestion() { return question; }
    public String getReponse() { return reponse; }
}
