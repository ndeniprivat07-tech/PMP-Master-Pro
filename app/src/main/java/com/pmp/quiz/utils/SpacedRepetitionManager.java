package com.pmp.quiz.utils;

import com.pmp.quiz.models.Flashcard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpacedRepetitionManager {

    public void reviewCard(Flashcard card, int quality) {
        if (quality < 3) {
            card.setRepetitions(0);
            card.setIntervalJours(1);
        } else {
            card.setRepetitions(card.getRepetitions() + 1);
            if (card.getRepetitions() == 1) {
                card.setIntervalJours(1);
            } else if (card.getRepetitions() == 2) {
                card.setIntervalJours(6);
            } else {
                card.setIntervalJours((int) Math.round(card.getIntervalJours() * card.getEaseFactor()));
            }
            double newEF = card.getEaseFactor() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
            card.setEaseFactor(Math.max(1.3, newEF));
        }
        card.setDerniereRevision(System.currentTimeMillis());
        if (card.getIntervalJours() > 30) card.setMaitrise(true);
    }

    public List<Flashcard> getCardsToReview(List<Flashcard> allCards) {
        List<Flashcard> toReview = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Flashcard card : allCards) {
            if (card.isMaitrise()) continue;
            long days = TimeUnit.MILLISECONDS.toDays(now - card.getDerniereRevision());
            if (days >= card.getIntervalJours()) toReview.add(card);
        }
        Collections.sort(toReview, (c1, c2) -> Long.compare(c1.getDerniereRevision(), c2.getDerniereRevision()));
        return toReview;
    }

    public int getProgressPercentage(List<Flashcard> allCards) {
        if (allCards.isEmpty()) return 0;
        int mastered = 0;
        for (Flashcard card : allCards) { if (card.isMaitrise()) mastered++; }
        return (mastered * 100) / allCards.size();
    }
}
