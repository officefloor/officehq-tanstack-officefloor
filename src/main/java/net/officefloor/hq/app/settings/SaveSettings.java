package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** A dated setting is recorded from its from-date, else from the sheet's today. */
public class SaveSettings {
    public void save(@Val SettingsChange change, Settings settings, Sheets sheets) {
        String sheetId = change.sheetId();
        LocalDate today = sheets.today(sheetId);
        if (change.setsClerkLimit()) setDated(settings, change, today, Settings.CLERK_LIMIT, change.clerkLimit());
        if (change.floor() != null) setDated(settings, change, today, Settings.FLOOR, change.floor());
        if (change.terms() != null) setDated(settings, change, today, Settings.TERMS, BigDecimal.valueOf(change.terms()));
        change.gradeTerms().forEach((grade, terms) -> setDated(settings, change, today, Settings.gradeKey(grade),
                terms != null ? BigDecimal.valueOf(terms) : null));
        if (change.latePct() != null) setDated(settings, change, today, Settings.LATE_PCT, change.latePct());
        if (change.setsPolicyDate()) settings.setPolicyDate(sheetId, change.policyDate());
        if (change.setsCap()) setDated(settings, change, today, Settings.CAP, change.cap());
        if (change.setsProofThreshold()) setDated(settings, change, today, Settings.PROOF_THRESHOLD, change.proofThreshold());
        if (change.setsNoteThreshold()) setDated(settings, change, today, Settings.NOTE_THRESHOLD, change.noteThreshold());
        if (change.setsPeriodNumberingFrom()) settings.setPeriodNumberingFrom(sheetId, change.periodNumberingFrom());
        if (change.setsGraceDays()) settings.setGraceDays(sheetId, change.graceDays());
        change.perms().forEach((action, roles) -> settings.setPermission(sheetId, action, roles));
    }

    private static void setDated(Settings settings, SettingsChange change, LocalDate today, String key, BigDecimal value) {
        settings.setDated(change.sheetId(), key, change.froms().getOrDefault(key, today), value);
    }
}
