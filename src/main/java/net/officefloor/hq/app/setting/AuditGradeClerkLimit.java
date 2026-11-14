package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each grade's clerk limit just saved, by grade (blank: none), even when it is the one the grade had. */
public class AuditGradeClerkLimit {
    public void audit(@Val GradeClerkLimit limit, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        limit.values().forEach((grade, value) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=" + DatedSettings.gradeClerkLimit(grade) + " value=" + (value == null ? "" : value.toPlainString())
                + DatedSettings.auditFrom(limit.fromOf(grade))));
    }
}
