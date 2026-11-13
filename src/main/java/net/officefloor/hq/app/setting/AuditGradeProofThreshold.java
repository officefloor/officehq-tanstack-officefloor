package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each grade's proof threshold just saved, by grade (blank: none), even when it is the one the grade had. */
public class AuditGradeProofThreshold {
    public void audit(@Val GradeProofThreshold threshold, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        threshold.values().forEach((grade, value) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=" + DatedSettings.gradeProofThreshold(grade) + " value=" + (value == null ? "" : value.toPlainString())
                + DatedSettings.auditFrom(threshold.fromOf(grade))));
    }
}
