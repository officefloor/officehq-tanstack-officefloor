package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sheet's settings, as the screen reads them, each figure setting as it is in force on the sheet's
 * today: the clerk limit with two decimals ('' for none), the floor with two decimals, the terms in
 * days and the late percent with two decimals, and the policy date as yyyy-MM-dd ('' for the sheet's
 * today), and the period numbering from as yyyy-MM-dd ('' for off), and each grade's own terms in
 * days ('' for the sheet's terms), and the cap with two decimals ('' for none); and every value
 * recorded for each figure setting, by key, in from-date order; and the proof threshold with two
 * decimals ('' for none); and each action's entry in the permission matrix, in matrix order, as
 * saved ('' for the standing rule); and the grace days ('' for off); and the note threshold with two
 * decimals ('' for none); and the entry date as yyyy-MM-dd ('' for off); and each grade's grace
 * days ('' for none); and each grade's proof threshold with two decimals ('' for none); and the pot
 * with two decimals ('' for none); and the parent sheet's id ('' for none); and the fiscal year start
 * month, 1 to 12; and the alert threshold with two decimals ('' for off); and the rounding mode,
 * half-up or half-even ('' for none); and the review threshold with two decimals ('' for none); and the escalation gap in days ('' for off); and the review SLA in days ('' for none); and the proof kinds, a comma separated list ('' for none); and the default grade ('' for A); and the score thresholds, mid and high ('' for none); and the score log switch (on, '' for off); and each grade's own clerk limit with two decimals ('' for none); and the maximum of open claims per person ('' for none).
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, Map<String, String> gradeTerms, String cap, Map<String, List<Dated>> dated,
        String proofThreshold, Map<String, String> perms, String graceDays, String noteThreshold,
        String entryDate, Map<String, String> gradeGrace, Map<String, String> gradeProofThreshold,
        String pot, String parent, String fiscalStartMonth, String alertThreshold, String rounding,
        String reviewThreshold, String escalateAfter, String reviewSla, String proofKinds, String defaultGrade,
        String scoreMid, String scoreHigh, String scoreLog, Map<String, String> gradeClerkLimit, String maxOpen,
        String pendingSla) {

    /** A value ('' for none) recorded from a date (yyyy-MM-dd). */
    public record Dated(String from, String value) {
    }

    public static SettingsResponse from(Settings settings, String parent, java.util.List<String> grades) {
        LocalDate today = settings.today();
        return new SettingsResponse(show(settings.clerkLimitOn(today)),
                show(settings.floorOn(today)), Integer.toString(settings.termsOn(today)),
                show(settings.latePctOn(today)),
                settings.policyDate() == null ? "" : settings.policyDate().toString(),
                settings.periodNumberingFrom() == null ? "" : settings.periodNumberingFrom().toString(),
                gradeTerms(settings, grades),
                show(settings.capOn(today)),
                dated(settings),
                show(settings.proofThresholdOn(today)),
                perms(settings),
                settings.graceDays() == null ? "" : settings.graceDays().toString(),
                show(settings.noteThresholdOn(today)),
                settings.entryDate() == null ? "" : settings.entryDate().toString(),
                gradeGrace(settings, grades),
                gradeProofThreshold(settings, grades),
                show(settings.pot()), parent == null ? "" : parent,
                Integer.toString(settings.fiscalStart()), show(settings.alertThreshold()),
                rounding(settings.valueOn(Settings.ROUNDING, today)),
                show(settings.reviewThresholdOn(today)),
                settings.escalateAfter() == null ? "" : settings.escalateAfter().toString(),
                settings.reviewSla() == null ? "" : settings.reviewSla().toString(),
                settings.proofKinds() == null ? "" : settings.proofKinds(),
                settings.defaultGrade() == null ? "" : settings.defaultGrade(),
                settings.scoreMid() == null ? "" : settings.scoreMid().toString(),
                settings.scoreHigh() == null ? "" : settings.scoreHigh().toString(),
                settings.scoreLog() ? Settings.ON : "",
                gradeClerkLimit(settings, grades),
                settings.maxOpen() == null ? "" : settings.maxOpen().toString(),
                settings.pendingSla() == null ? "" : settings.pendingSla().toString());
    }

    private static String rounding(String value) {
        return value == null ? "" : value;
    }

    private static String show(java.math.BigDecimal figure) {
        return figure == null ? "" : figure.setScale(2).toPlainString();
    }

    private static Map<String, String> gradeTerms(Settings settings, java.util.List<String> grades) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : grades) {
            Integer own = settings.gradeTermsOn(grade, settings.today());
            shown.put(grade, own == null ? "" : own.toString());
        }
        return shown;
    }

    private static Map<String, String> gradeGrace(Settings settings, java.util.List<String> grades) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : grades) {
            Integer grace = settings.gradeGraceOn(grade, settings.today());
            shown.put(grade, grace == null ? "" : grace.toString());
        }
        return shown;
    }

    private static Map<String, String> gradeProofThreshold(Settings settings, java.util.List<String> grades) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : grades) shown.put(grade, show(settings.gradeProofThresholdOn(grade, settings.today())));
        return shown;
    }

    private static Map<String, String> gradeClerkLimit(Settings settings, java.util.List<String> grades) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String grade : grades) shown.put(grade, show(settings.gradeClerkLimitOn(grade, settings.today())));
        return shown;
    }

    private static Map<String, String> perms(Settings settings) {
        Map<String, String> shown = new LinkedHashMap<>();
        for (String action : Settings.ACTIONS) {
            String roles = settings.perms() == null ? null : settings.perms().get(action);
            shown.put(action, roles == null ? "" : roles);
        }
        return shown;
    }

    private static Map<String, List<Dated>> dated(Settings settings) {
        Map<String, List<Dated>> shown = new LinkedHashMap<>();
        settings.dated().forEach((key, values) -> {
            List<Dated> list = new ArrayList<>();
            values.forEach((from, value) -> list.add(new Dated(from.toString(), value)));
            shown.put(key, list);
        });
        return shown;
    }
}
