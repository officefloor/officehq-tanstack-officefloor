package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSettingChanged {
    // One line per setting saved: the clerk limit, then the floor, then the terms, then each grade's own terms by grade, then the late percent, then the policy date, then the period numbering from-date, then the cap. A dated setting saved with a from-date ends with from=<date>.
    public void audit(@Val SheetSettings settings, ServerHttpConnection connection, Audit audit) {
        if (settings.clerkLimit() != null) {
            audit.record(dated(settings, connection,
                    "key=" + Settings.CLERK_LIMIT, "value=" + settings.clerkLimit(), from(settings, Settings.CLERK_LIMIT)));
        }
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
