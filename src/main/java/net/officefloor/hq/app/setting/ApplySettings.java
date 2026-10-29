package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplySettings {
    // A dated setting is recorded from its from-date, else from the sheet's today, keeping the values recorded from other dates.
    public void apply(@Val SheetSettings settings, Settings store, Sheets sheets) {
        LocalDate today = sheets.today(settings.sheetId());
        if (settings.clerkLimit() != null) {
            dated(settings, store, today, Settings.CLERK_LIMIT, settings.clerkLimit());
        }
        settings.gradeClerkLimit().forEach((grade, amount) -> dated(settings, store, today, Settings.gradeClerkLimitKey(grade), amount));
        if (settings.floor() != null) {
            dated(settings, store, today, Settings.FLOOR, settings.floor());
        }
        if (settings.terms() != null) {
            dated(settings, store, today, Settings.TERMS, settings.terms());
        }
        settings.gradeTerms().forEach((grade, days) -> dated(settings, store, today, Settings.gradeTermsKey(grade), days));
        if (settings.latePct() != null) {
            dated(settings, store, today, Settings.LATE_PCT, settings.latePct());
        }
        if (settings.policyDate() != null) {
            store.set(settings.sheetId(), Settings.POLICY_DATE, settings.policyDate());
        }
        if (settings.periodNumberingFrom() != null) {
            store.set(settings.sheetId(), Settings.PERIOD_NUMBERING_FROM, settings.periodNumberingFrom());
        }
        if (settings.cap() != null) {
            dated(settings, store, today, Settings.CAP, settings.cap());
        }
        if (settings.proofThreshold() != null) {
            dated(settings, store, today, Settings.PROOF_THRESHOLD, settings.proofThreshold());
        }
        settings.gradeProof().forEach((grade, amount) -> dated(settings, store, today, Settings.gradeProofKey(grade), amount));
        if (settings.noteThreshold() != null) {
            dated(settings, store, today, Settings.NOTE_THRESHOLD, settings.noteThreshold());
        }
        if (settings.graceDays() != null) {
            store.set(settings.sheetId(), Settings.GRACE_DAYS, settings.graceDays());
        }
        settings.gradeGrace().forEach((grade, days) -> dated(settings, store, today, Settings.gradeGraceKey(grade), days));
        if (settings.entryDate() != null) {
            store.set(settings.sheetId(), Settings.ENTRY_DATE, settings.entryDate());
        }
        settings.perms().forEach((action, roles) -> store.set(settings.sheetId(), Settings.permKey(action), roles));
        if (settings.pot() != null) {
            store.set(settings.sheetId(), Settings.POT, settings.pot());
        }
        if (settings.alertThreshold() != null) {
            store.set(settings.sheetId(), Settings.ALERT_THRESHOLD, settings.alertThreshold());
        }
        if (settings.parent() != null) {
            store.set(settings.sheetId(), Settings.PARENT, settings.parent());
        }
        if (settings.fiscalStartMonth() != null) {
            store.set(settings.sheetId(), Settings.FISCAL_START_MONTH, settings.fiscalStartMonth());
        }
        if (settings.rounding() != null) {
            dated(settings, store, today, Settings.ROUNDING, settings.rounding());
        }
        if (settings.reviewThreshold() != null) {
            dated(settings, store, today, Settings.REVIEW_THRESHOLD, settings.reviewThreshold());
        }
        if (settings.escalateAfter() != null) {
            store.set(settings.sheetId(), Settings.ESCALATE_AFTER, settings.escalateAfter());
        }
        if (settings.reviewSla() != null) {
            store.set(settings.sheetId(), Settings.REVIEW_SLA, settings.reviewSla());
        }
        if (settings.scoreMid() != null) {
            store.set(settings.sheetId(), Settings.SCORE_MID, settings.scoreMid());
        }
        if (settings.scoreHigh() != null) {
            store.set(settings.sheetId(), Settings.SCORE_HIGH, settings.scoreHigh());
        }
        if (settings.scoreLog() != null) {
            store.set(settings.sheetId(), Settings.SCORE_LOG, settings.scoreLog());
        }
        if (settings.proofKinds() != null) {
            store.set(settings.sheetId(), Settings.PROOF_KINDS, settings.proofKinds());
        }
        if (settings.maxOpen() != null) {
            store.set(settings.sheetId(), Settings.MAX_OPEN, settings.maxOpen());
        }
        if (settings.defaultGrade() != null) {
            store.set(settings.sheetId(), Settings.DEFAULT_GRADE, settings.defaultGrade());
        }
    }

    private static void dated(SheetSettings settings, Settings store, LocalDate today, String key, String value) {
        store.set(settings.sheetId(), key, settings.from().getOrDefault(key, today), value);
    }
}
