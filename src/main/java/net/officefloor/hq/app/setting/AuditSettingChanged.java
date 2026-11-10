package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records each setting saved, with the value saved (blank for no clerk limit): the clerk limit, then
 * the floor, then the terms, then the late percent, then the policy date, then the period numbering
 * from, then each grade's terms (blank for the sheet's), then the cap (blank for none), then the proof threshold (blank for none); a figure
 * setting saved from a from-date given ends with it. Runs after every rule has passed, so a refused
 * change writes nothing.
 */
public class AuditSettingChanged {
    public void audit(@Val Sheet sheet, @Val SettingsInput input, @Val Settings settings,
            HttpServletRequest request, Audit audit) {
        String head = "SETTING_CHANGED sheet=" + sheet.id() + " by=" + Audit.user(request);
        input.datedValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        if (input.policyDateChanged()) {
            audit.record(head + " key=policy-date value=" + (input.policyDate() == null ? "" : input.policyDate()));
        }
        if (input.periodNumberingFromChanged()) {
            audit.record(head + " key=period-numbering-from value="
                    + (input.periodNumberingFrom() == null ? "" : input.periodNumberingFrom()));
        }
        input.gradeTermsValues().forEach((key, value) -> audit.record(line(head, key, value, input)));
        if (input.capValue() != null) audit.record(line(head, Settings.CAP, input.capValue(), input));
        if (input.proofThresholdValue() != null) {
            audit.record(line(head, Settings.PROOF_THRESHOLD, input.proofThresholdValue(), input));
        }
    }

    private static String line(String head, String key, String value, SettingsInput input) {
        LocalDate from = input.from().get(key);
        return head + " key=" + key + " value=" + value + (from == null ? "" : " from=" + from);
    }
}
