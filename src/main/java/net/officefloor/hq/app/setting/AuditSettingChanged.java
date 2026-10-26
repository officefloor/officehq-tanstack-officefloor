package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSettingChanged {
    // One line per setting saved: the clerk limit, then the floor, then the terms, then the late percent, then the policy date, then the period numbering from-date.
    public void audit(@Val SheetSettings settings, ServerHttpConnection connection, Audit audit) {
        if (settings.clerkLimit() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.CLERK_LIMIT, "value=" + settings.clerkLimit()));
        }
        if (settings.floor() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.FLOOR, "value=" + settings.floor()));
        }
        if (settings.terms() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.TERMS, "value=" + settings.terms()));
        }
        if (settings.latePct() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.LATE_PCT, "value=" + settings.latePct()));
        }
        if (settings.policyDate() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.POLICY_DATE, "value=" + settings.policyDate()));
        }
        if (settings.periodNumberingFrom() != null) {
            audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                    "key=" + Settings.PERIOD_NUMBERING_FROM, "value=" + settings.periodNumberingFrom()));
        }
    }
}
