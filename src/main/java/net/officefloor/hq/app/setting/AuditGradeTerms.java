package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each grade's terms just saved, by grade; a grade left without its own records the sheet's terms it then uses (in force on the from-date). */
public class AuditGradeTerms {
    public void audit(@Val GradeTerms terms, @Val Sheet sheet, @RequestParam("user") String user, SettingRepository repository,
            Audit audit) {
        DatedSettings dated = repository.dated(sheet.id());
        terms.values().forEach((grade, value) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=terms-" + grade + " value="
                + dated.termsFor(grade, DatedSettings.fromOr(terms.fromOf(grade), sheet.today()))
                + DatedSettings.auditFrom(terms.fromOf(grade))));
    }
}
