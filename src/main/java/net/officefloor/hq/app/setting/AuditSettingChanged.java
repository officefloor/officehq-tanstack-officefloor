package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSettingChanged {
    // One line per setting saved: the clerk limit, then each grade's clerk limit by grade, then the floor, then the terms, then each grade's own terms by grade, then the late percent, then the policy date, then the period numbering from-date, then the cap, then the proof threshold, then each grade's proof threshold by grade, then the note threshold, then the grace days, then each grade's grace days by grade, then the entry date, then each action's permitted roles, in the matrix's order, then the pot, then the parent, then the fiscal year start month, then the alert threshold, then the rounding mode, then the review threshold, then the escalation gap, then the review SLA, then the proof kinds, then the default grade. A dated setting saved with a from-date ends with from=<date>.
    public void audit(@Val SheetSettings settings, ServerHttpConnection connection, Audit audit) {
        if (settings.clerkLimit() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.CLERK_LIMIT, "value=" + settings.clerkLimit(), from(settings, Settings.CLERK_LIMIT)));
        }
        settings.gradeClerkLimit().forEach((grade, amount) -> audit.record(dated(settings, connection,
                "key=" + Settings.gradeClerkLimitKey(grade), "value=" + amount, from(settings, Settings.gradeClerkLimitKey(grade)))));
        if (settings.floor() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.FLOOR, "value=" + settings.floor(), from(settings, Settings.FLOOR)));
        }
        if (settings.terms() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.TERMS, "value=" + settings.terms(), from(settings, Settings.TERMS)));
        }
        settings.gradeTerms().forEach((grade, days) -> audit.record(dated(settings, connection,
                "key=" + Settings.gradeTermsKey(grade), "value=" + days, from(settings, Settings.gradeTermsKey(grade)))));
        if (settings.latePct() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.LATE_PCT, "value=" + settings.latePct(), from(settings, Settings.LATE_PCT)));
        }
        if (settings.policyDate() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.POLICY_DATE, "value=" + settings.policyDate()));
        }
        if (settings.periodNumberingFrom() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.PERIOD_NUMBERING_FROM, "value=" + settings.periodNumberingFrom()));
        }
        if (settings.cap() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.CAP, "value=" + settings.cap(), from(settings, Settings.CAP)));
        }
        if (settings.proofThreshold() != null) {
            audit.record(dated(settings, connection, "key=" + Settings.PROOF_THRESHOLD, "value=" + settings.proofThreshold(),
                    from(settings, Settings.PROOF_THRESHOLD)));
        }
        settings.gradeProof().forEach((grade, amount) -> audit.record(dated(settings, connection,
                "key=" + Settings.gradeProofKey(grade), "value=" + amount, from(settings, Settings.gradeProofKey(grade)))));
        if (settings.noteThreshold() != null) {
            audit.record(dated(settings, connection, "key=" + Settings.NOTE_THRESHOLD, "value=" + settings.noteThreshold(),
                    from(settings, Settings.NOTE_THRESHOLD)));
        }
        if (settings.graceDays() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.GRACE_DAYS, "value=" + settings.graceDays()));
        }
        settings.gradeGrace().forEach((grade, days) -> audit.record(dated(settings, connection,
                "key=" + Settings.gradeGraceKey(grade), "value=" + days, from(settings, Settings.gradeGraceKey(grade)))));
        if (settings.entryDate() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.ENTRY_DATE, "value=" + settings.entryDate()));
        }
        settings.perms().forEach((action, roles) -> audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(),
                Audit.user(connection), "key=" + Settings.permKey(action), "value=" + roles)));
        if (settings.pot() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.POT, "value=" + settings.pot()));
        }
        if (settings.parent() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.PARENT, "value=" + settings.parent()));
        }
        if (settings.fiscalStartMonth() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.FISCAL_START_MONTH, "value=" + settings.fiscalStartMonth()));
        }
        if (settings.alertThreshold() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.ALERT_THRESHOLD, "value=" + settings.alertThreshold()));
        }
        if (settings.rounding() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.ROUNDING, "value=" + settings.rounding(), from(settings, Settings.ROUNDING)));
        }
        if (settings.reviewThreshold() != null) {
            audit.record(dated(settings, connection, "key=" + Settings.REVIEW_THRESHOLD, "value=" + settings.reviewThreshold(),
                    from(settings, Settings.REVIEW_THRESHOLD)));
        }
        if (settings.escalateAfter() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.ESCALATE_AFTER, "value=" + settings.escalateAfter()));
        }
        if (settings.reviewSla() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.REVIEW_SLA, "value=" + settings.reviewSla()));
        }
        if (settings.scoreMid() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.SCORE_MID, "value=" + settings.scoreMid()));
        }
        if (settings.scoreHigh() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.SCORE_HIGH, "value=" + settings.scoreHigh()));
        }
        if (settings.proofKinds() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.PROOF_KINDS, "value=" + settings.proofKinds()));
        }
        if (settings.defaultGrade() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.DEFAULT_GRADE, "value=" + settings.defaultGrade()));
        }
    }

    private static String from(SheetSettings settings, String key) {
        java.time.LocalDate from = settings.from().get(key);
        return from == null ? null : "from=" + from;
    }

    /** A dated setting's line: its key and value, then its from-date when one was given. */
    private static String dated(SheetSettings settings, ServerHttpConnection connection, String key, String value, String from) {
        String line = Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection), key, value);
        return from == null ? line : line + " " + from;
    }
}
