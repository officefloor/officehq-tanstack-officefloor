package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;

public class SaveSettings {
    public void save(@Val SettingsChange change, Settings settings) {
        if (change.setsClerkLimit()) settings.setClerkLimit(change.sheetId(), change.clerkLimit());
        if (change.floor() != null) settings.setFloor(change.sheetId(), change.floor());
        if (change.terms() != null) settings.setTerms(change.sheetId(), change.terms());
        if (change.latePct() != null) settings.setLatePct(change.sheetId(), change.latePct());
        if (change.setsPolicyDate()) settings.setPolicyDate(change.sheetId(), change.policyDate());
        if (change.setsPeriodNumberingFrom()) settings.setPeriodNumberingFrom(change.sheetId(), change.periodNumberingFrom());
    }
}
