package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the settings are saved, so a refused change writes no audit line. A line per setting
 *  set, a dated one ending in its from-date when one was given. */
public class RecordSettingChanged {
    public void record(@Val SettingsChange change, Audit audit) {
        String prefix = "SETTING_CHANGED sheet=" + change.sheetId() + " by=" + change.by();
        if (change.setsClerkLimit()) {
            audit.record(prefix + " key=clerk-limit value="
                    + (change.clerkLimit() != null ? change.clerkLimit().toPlainString() : "")
                    + from(change, Settings.CLERK_LIMIT));
        }
        if (change.floor() != null) {
            audit.record(prefix + " key=floor value=" + change.floor().toPlainString() + from(change, Settings.FLOOR));
        }
        if (change.terms() != null) {
            audit.record(prefix + " key=terms value=" + change.terms() + from(change, Settings.TERMS));
        }
        change.gradeTerms().forEach((grade, terms) ->
                audit.record(prefix + " key=terms-" + grade + " value=" + (terms != null ? terms : "")
                        + from(change, Settings.gradeKey(grade))));
        if (change.latePct() != null) {
            audit.record(prefix + " key=late-pct value=" + change.latePct().toPlainString() + from(change, Settings.LATE_PCT));
        }
        if (change.setsPolicyDate()) {
            audit.record(prefix + " key=policy-date value="
                    + (change.policyDate() != null ? change.policyDate().toString() : ""));
        }
        if (change.setsPeriodNumberingFrom()) {
            audit.record(prefix + " key=period-numbering-from value="
                    + (change.periodNumberingFrom() != null ? change.periodNumberingFrom().toString() : ""));
        }
        if (change.setsCap()) {
            audit.record(prefix + " key=cap value=" + (change.cap() != null ? change.cap().toPlainString() : "")
                    + from(change, Settings.CAP));
        }
        if (change.setsProofThreshold()) {
            audit.record(prefix + " key=proof-threshold value="
                    + (change.proofThreshold() != null ? change.proofThreshold().toPlainString() : "")
                    + from(change, Settings.PROOF_THRESHOLD));
        }
        change.perms().forEach((action, roles) ->
                audit.record(prefix + " key=" + Settings.permKey(action) + " value=" + (roles != null ? roles : "")));
        if (change.setsGraceDays()) {
            audit.record(prefix + " key=grace-days value=" + (change.graceDays() != null ? change.graceDays() : ""));
        }
    }

    /** " from=<date>" when a from-date was given for the key, else nothing. */
    private static String from(SettingsChange change, String key) {
        LocalDate from = change.froms().get(key);
        return from != null ? " from=" + from : "";
    }
}
