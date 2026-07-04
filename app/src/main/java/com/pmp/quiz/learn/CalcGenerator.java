package com.pmp.quiz.learn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Générateur de questions de calcul illimitées, mathématiquement justes :
 * EVM (CPI, SPI, EAC, CV, SV, TCPI), PERT, canaux de communication, EMV, VAN.
 * Les distracteurs sont construits à partir des erreurs classiques des candidats.
 */
public class CalcGenerator {

    private static final Random rnd = new Random();

    public static ContentManager.QItem next() {
        switch (rnd.nextInt(12)) {
            case 0: return genCPI();
            case 1: return genSPI();
            case 2: return genEAC();
            case 3: return genCV();
            case 4: return genPERT();
            case 5: return genCanaux();
            case 6: return genEMV();
            case 7: return genTCPI();
            case 8: return genCheminCritique();
            case 9: return genMargeTotale();
            case 10: return genLag();
            default: return genMargeImpact();
        }
    }

    private static ContentManager.QItem make(String q, double bonne, double[] distracteurs,
                                             String unite, String explication) {
        ContentManager.QItem item = new ContentManager.QItem();
        item.q = q;
        List<String> opts = new ArrayList<>();
        opts.add(fmt(bonne) + unite);
        for (double d : distracteurs) opts.add(fmt(d) + unite);
        Collections.shuffle(opts);
        item.options = opts.toArray(new String[0]);
        item.correct = opts.indexOf(fmt(bonne) + unite);
        item.explication = explication;
        return item;
    }

    private static String fmt(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.FRANCE, "%.2f", v);
    }

    private static ContentManager.QItem genCPI() {
        int ev = (2 + rnd.nextInt(8)) * 50;          // 100..450
        int ac = ev + (rnd.nextInt(11) - 5) * 20;    // autour de EV
        if (ac == ev) ac += 20;
        if (ac <= 0) ac = ev + 40;
        double cpi = round2((double) ev / ac);
        return make(
                "Votre projet affiche EV = " + ev + " k€ et AC = " + ac + " k€. Quel est le CPI et que signifie-t-il ?",
                cpi,
                new double[]{round2((double) ac / ev), round2(cpi + 0.2), round2(Math.max(0.1, cpi - 0.2))},
                "",
                "CPI = EV / AC = " + ev + " / " + ac + " = " + fmt(cpi)
                        + (cpi < 1 ? " (< 1 : chaque euro dépensé produit moins d'un euro de valeur → dépassement de budget)."
                                    : " (≥ 1 : performance des coûts bonne ou conforme).")
                        + " Piège classique : inverser la formule (AC/EV).");
    }

    private static ContentManager.QItem genSPI() {
        int ev = (2 + rnd.nextInt(8)) * 50;
        int pv = ev + (rnd.nextInt(11) - 5) * 20;
        if (pv == ev) pv += 20;
        if (pv <= 0) pv = ev + 40;
        double spi = round2((double) ev / pv);
        return make(
                "EV = " + ev + " k€, PV = " + pv + " k€. Quel est le SPI ?",
                spi,
                new double[]{round2((double) pv / ev), round2(spi + 0.15), round2(Math.max(0.1, spi - 0.15))},
                "",
                "SPI = EV / PV = " + ev + " / " + pv + " = " + fmt(spi)
                        + (spi < 1 ? " (< 1 : moins de travail accompli que prévu → retard planning)."
                                    : " (≥ 1 : en avance ou conforme au planning)."));
    }

    private static ContentManager.QItem genEAC() {
        int bac = (4 + rnd.nextInt(8)) * 100;       // 400..1100
        double cpi = round2(0.7 + rnd.nextInt(5) * 0.1); // 0.7..1.1
        double eac = Math.round(bac / cpi);
        return make(
                "BAC = " + bac + " k€ et CPI = " + fmt(cpi) + " (tendance stable). Quelle est l'EAC (estimation du coût final) ?",
                eac,
                new double[]{Math.round(bac * cpi), bac, Math.round(bac / cpi * 1.1)},
                " k€",
                "EAC = BAC / CPI = " + bac + " / " + fmt(cpi) + " = " + fmt(eac)
                        + " k€. Si la performance actuelle continue, le projet coûtera ce montant. Piège : multiplier au lieu de diviser.");
    }

    private static ContentManager.QItem genCV() {
        int ev = (3 + rnd.nextInt(7)) * 40;
        int ac = ev + (rnd.nextInt(9) - 4) * 25;
        if (ac == ev) ac += 25;
        double cv = ev - ac;
        return make(
                "EV = " + ev + " k€, AC = " + ac + " k€. Quel est le CV (écart de coût) ?",
                cv,
                new double[]{ac - ev, ev + ac, round2((double) ev / ac)},
                (Math.abs(cv) < 5 ? "" : " k€"),
                "CV = EV − AC = " + ev + " − " + ac + " = " + fmt(cv) + " k€. "
                        + (cv < 0 ? "Négatif : le projet dépense plus que la valeur produite (dépassement)."
                                   : "Positif : sous le budget.")
                        + " Les écarts se calculent toujours avec EV en premier.");
    }

    private static ContentManager.QItem genPERT() {
        int o = 2 + rnd.nextInt(5);          // 2..6
        int m = o + 2 + rnd.nextInt(4);      // o+2..o+5
        int p = m + (6 - ((o + 4 * m) % 6) % 6); // ajuste pour un résultat entier
        while ((o + 4 * m + p) % 6 != 0) p++;
        double res = (o + 4.0 * m + p) / 6;
        return make(
                "Estimation à trois points (PERT) : optimiste = " + o + " j, plus probable = " + m
                        + " j, pessimiste = " + p + " j. Quelle est la durée attendue ?",
                res,
                new double[]{round2((o + m + p) / 3.0), m, round2(res + 1)},
                " j",
                "PERT = (O + 4M + P) / 6 = (" + o + " + " + (4 * m) + " + " + p + ") / 6 = " + fmt(res)
                        + " j. Piège classique : la moyenne simple (O+M+P)/3, qui ignore la pondération du cas probable.");
    }

    private static ContentManager.QItem genCanaux() {
        int n1 = 4 + rnd.nextInt(7);         // 4..10
        int n2 = n1 + 2 + rnd.nextInt(4);    // +2..+5
        int c1 = n1 * (n1 - 1) / 2;
        int c2 = n2 * (n2 - 1) / 2;
        int diff = c2 - c1;
        return make(
                "Votre équipe passe de " + n1 + " à " + n2 + " personnes. Combien de canaux de communication SUPPLÉMENTAIRES cela crée-t-il ?",
                diff,
                new double[]{c2, n2 - n1, c1},
                "",
                "Canaux = n(n−1)/2. Avant : " + n1 + "×" + (n1 - 1) + "/2 = " + c1
                        + ". Après : " + n2 + "×" + (n2 - 1) + "/2 = " + c2
                        + ". Supplément : " + c2 + " − " + c1 + " = " + diff
                        + ". Piège : donner le total au lieu de la différence.");
    }

    private static ContentManager.QItem genEMV() {
        int proba = (1 + rnd.nextInt(6)) * 10;      // 10..60%
        int impact = (2 + rnd.nextInt(9)) * 10;     // 20..100 k€
        boolean menace = rnd.nextBoolean();
        double emv = round2(proba / 100.0 * impact * (menace ? -1 : 1));
        return make(
                "Un " + (menace ? "risque (menace)" : "risque positif (opportunité)") + " a " + proba
                        + "% de probabilité et un impact de " + (menace ? "−" : "+") + impact
                        + " k€. Quelle est sa valeur monétaire attendue (EMV) ?",
                emv,
                new double[]{-emv, (menace ? -1 : 1) * impact, round2(emv / 2)},
                " k€",
                "EMV = probabilité × impact = " + (proba / 100.0) + " × " + (menace ? "−" : "+") + impact
                        + " = " + fmt(emv) + " k€. Le signe suit la nature du risque : négatif pour une menace, positif pour une opportunité.");
    }

    private static ContentManager.QItem genTCPI() {
        int bac = (5 + rnd.nextInt(6)) * 100;
        int ev = (int) (bac * (0.3 + rnd.nextInt(3) * 0.1));
        int ac = ev + (1 + rnd.nextInt(3)) * 25;
        double tcpi = round2((double) (bac - ev) / (bac - ac));
        return make(
                "BAC = " + bac + " k€, EV = " + ev + " k€, AC = " + ac + " k€. Quel est le TCPI (performance requise pour tenir le budget) ?",
                tcpi,
                new double[]{round2((double) ev / ac), round2((double) (bac - ac) / (bac - ev)), round2(tcpi - 0.15)},
                "",
                "TCPI = (BAC − EV) / (BAC − AC) = (" + (bac - ev) + ") / (" + (bac - ac) + ") = " + fmt(tcpi)
                        + (tcpi > 1 ? " (> 1 : il faudra être plus performant que prévu sur le travail restant — alerte)."
                                     : " (≤ 1 : le budget reste tenable au rythme actuel)."));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ===== QCM DE PLANNING / ORDONNANCEMENT =====

    /** Réseau simple : A -> (B et C en parallèle) -> D. Durée du chemin critique ? */
    private static ContentManager.QItem genCheminCritique() {
        int a = 2 + rnd.nextInt(5);
        int b = 3 + rnd.nextInt(8);
        int c = 3 + rnd.nextInt(8);
        while (c == b) c = 3 + rnd.nextInt(8);
        int d = 2 + rnd.nextInt(5);
        int critique = a + Math.max(b, c) + d;
        int autre = a + Math.min(b, c) + d;
        String brancheCritique = b > c ? "B" : "C";
        return make(
                "Réseau du projet : l'activité A (" + a + " j) est suivie de B (" + b + " j) et C (" + c
                        + " j) réalisées EN PARALLÈLE ; D (" + d + " j) ne peut commencer que lorsque B ET C sont terminées. "
                        + "Quelle est la durée du chemin critique ?",
                critique,
                new double[]{autre, a + b + c + d, critique - 1},
                " j",
                "Le chemin critique passe par la branche la plus LONGUE : A(" + a + ") + " + brancheCritique
                        + "(" + Math.max(b, c) + ") + D(" + d + ") = " + critique
                        + " j. Piège : additionner toutes les activités (" + (a + b + c + d)
                        + " j) alors que B et C sont en parallèle.");
    }

    /** Marge totale = LS - ES (ou LF - EF) */
    private static ContentManager.QItem genMargeTotale() {
        int es = 3 + rnd.nextInt(10);
        int marge = 2 + rnd.nextInt(7);
        int ls = es + marge;
        int duree = 3 + rnd.nextInt(6);
        return make(
                "Une activité a : début au plus tôt (ES) = jour " + es + ", début au plus tard (LS) = jour " + ls
                        + ", durée = " + duree + " j. Quelle est sa marge totale (total float) ?",
                marge,
                new double[]{ls + duree - es, duree, 0},
                " j",
                "Marge totale = LS − ES = " + ls + " − " + es + " = " + marge
                        + " j. L'activité peut glisser de " + marge
                        + " j sans retarder le projet. Rappel : une marge de 0 = activité du chemin critique.");
    }

    /** Décalage (lag) sur une liaison Fin-Début */
    private static ContentManager.QItem genLag() {
        int finA = 5 + rnd.nextInt(15);
        int lag = 2 + rnd.nextInt(5);
        int dureeB = 3 + rnd.nextInt(6);
        int debutB = finA + lag;
        int finB = debutB + dureeB;
        return make(
                "L'activité B suit l'activité A avec une liaison Fin-Début et un décalage (lag) de +" + lag
                        + " j (ex. séchage du béton). A se termine au jour " + finA + " et B dure " + dureeB
                        + " j. À quel jour B se termine-t-elle ?",
                finB,
                new double[]{finA + dureeB, finB - lag, finB + lag},
                "",
                "Début de B = fin de A + lag = " + finA + " + " + lag + " = jour " + debutB
                        + ". Fin de B = " + debutB + " + " + dureeB + " = jour " + finB
                        + ". Piège : oublier le lag (réponse " + (finA + dureeB) + ").");
    }

    /** Impact d'un retard selon la marge disponible */
    private static ContentManager.QItem genMargeImpact() {
        int marge = 2 + rnd.nextInt(6);
        boolean depasse = rnd.nextBoolean();
        int retard = depasse ? marge + 1 + rnd.nextInt(4) : 1 + rnd.nextInt(marge);
        int impact = Math.max(0, retard - marge);
        return make(
                "Une activité HORS chemin critique dispose d'une marge totale de " + marge
                        + " j. Elle prend " + retard + " j de retard. De combien la date de fin du PROJET est-elle décalée ?",
                impact,
                new double[]{retard, marge, impact == 0 ? retard + marge : 0},
                " j",
                impact == 0
                        ? "Le retard (" + retard + " j) est inférieur ou égal à la marge (" + marge
                                + " j) : la fin du projet n'est PAS affectée (0 j). La marge absorbe le glissement."
                        : "Le retard (" + retard + " j) dépasse la marge (" + marge + " j) de " + impact
                                + " j : le projet glisse de " + impact
                                + " j — et cette activité intègre désormais le chemin critique.");
    }
}
