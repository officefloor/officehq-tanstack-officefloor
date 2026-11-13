package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the policy date just saved (blank: the sheet's today), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditPolicyDate {
    public void audit(@Val PolicyDate policyDate, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!policyDate.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=policy-date value="
                + (policyDate.value() == null ? "" : policyDate.value().toString()));
    }
}
