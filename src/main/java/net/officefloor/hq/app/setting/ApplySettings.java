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
    }

    private static void dated(SheetSettings settings, Settings store, LocalDate today, String key, String value) {
        store.set(settings.sheetId(), key, settings.from().getOrDefault(key, today), value);
    }
}
