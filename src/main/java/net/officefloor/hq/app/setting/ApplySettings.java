package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;

public class ApplySettings {
    public void apply(@Val SheetSettings settings, Settings store) {
        if (settings.clerkLimit() != null) {
            store.set(settings.sheetId(), Settings.CLERK_LIMIT, settings.clerkLimit());
        }
        if (settings.floor() != null) {
            store.set(settings.sheetId(), Settings.FLOOR, settings.floor());
        }
        if (settings.terms() != null) {
            store.set(settings.sheetId(), Settings.TERMS, settings.terms());
        }
        settings.gradeTerms().forEach((grade, days) -> store.set(settings.sheetId(), Settings.gradeTermsKey(grade), days));
        if (settings.latePct() != null) {
            store.set(settings.sheetId(), Settings.LATE_PCT, settings.latePct());
        }
        if (settings.policyDate() != null) {
            store.set(settings.sheetId(), Settings.POLICY_DATE, settings.policyDate());
        }
        if (settings.periodNumberingFrom() != null) {
            store.set(settings.sheetId(), Settings.PERIOD_NUMBERING_FROM, settings.periodNumberingFrom());
        }
    }
}
