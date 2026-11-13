package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each grade's grace days just saved, by grade (blank: none), even when they are the ones the grade had. */
public class AuditGradeGrace {
    public void audit(@Val GradeGrace grace, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        grace.values().forEach((grade, value) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=" + DatedSettings.gradeGrace(grade) + " value=" + (value == null ? "" : value.toString())
                + DatedSettings.auditFrom(grace.fromOf(grade))));
    }
}
