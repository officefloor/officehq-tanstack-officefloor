package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records each setting saved (blank for no clerk limit): the clerk limit, then the floor, then
 * the terms, then the late percent, then the policy date, then the period numbering from, then each
 * grade's terms (blank for the sheet's), then the cap (blank for none). Runs after
 * every rule has passed, so a refused change writes nothing.
 */
public class AuditSettingChanged {
    public void audit(@Val Sheet sheet, @Val SettingsInput input, @Val Settings settings,
            HttpServletRequest request, Audit audit) {
        String head = "SETTING_CHANGED sheet=" + sheet.id() + " by=" + Audit.user(request);
        SettingsResponse shown = SettingsResponse.from(settings);
        if (input.clerkLimitChanged()) audit.record(head + " key=clerk-limit value=" + shown.clerkLimit());
        if (input.floor() != null) audit.record(head + " key=floor value=" + shown.floor());
        if (input.terms() != null) audit.record(head + " key=terms value=" + shown.terms());
        if (input.latePct() != null) audit.record(head + " key=late-pct value=" + shown.latePct());
        if (input.policyDateChanged()) audit.record(head + " key=policy-date value=" + shown.policyDate());
        if (input.periodNumberingFromChanged()) {
            audit.record(head + " key=period-numbering-from value=" + shown.periodNumberingFrom());
        }
        for (String grade : input.gradeTerms().keySet()) {
            audit.record(head + " key=terms-" + grade + " value=" + shown.gradeTerms().get(grade));
        }
        if (input.capChanged()) audit.record(head + " key=cap value=" + shown.cap());
    }
}
