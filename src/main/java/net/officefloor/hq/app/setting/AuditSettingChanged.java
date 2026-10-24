package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records each setting the form set, once it is saved: the clerk limit (blank when there is none), then the floor, then the terms, then the late percent, then the policy date (blank when it is the sheet's today). */
public class AuditSettingChanged {
    public void audit(@Val SettingsInput input, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        SettingsResponse settings = SettingsResponse.from(sheet);
        String head = "SETTING_CHANGED sheet=" + sheet.getId() + " by=" + AuditUser.decode(user);
        if (input.setClerkLimit()) {
            audit.record(head + " key=clerk-limit value=" + (settings.clerkLimit() == null ? "" : settings.clerkLimit()));
        }
        if (input.setFloor()) {
            audit.record(head + " key=floor value=" + settings.floor());
        }
        if (input.setTerms()) {
            audit.record(head + " key=terms value=" + settings.terms());
        }
        if (input.setLatePct()) {
            audit.record(head + " key=late-pct value=" + settings.latePct());
        }
        if (input.setPolicyDate()) {
            audit.record(head + " key=policy-date value=" + settings.policyDate());
        }
    }
}
