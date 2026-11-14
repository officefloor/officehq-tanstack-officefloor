package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the review threshold just saved (blank: none), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditReviewThreshold {
    public void audit(@Val ReviewThreshold threshold, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!threshold.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=review-threshold value="
                + (threshold.value() == null ? "" : threshold.value().toPlainString())
                + DatedSettings.auditFrom(threshold.from()));
    }
}
