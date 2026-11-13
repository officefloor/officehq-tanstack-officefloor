package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each grade's terms just saved, by grade; a grade left without its own records the sheet's terms it now uses. */
public class AuditGradeTerms {
    public void audit(@Val GradeTerms terms, @Val Sheet sheet, @RequestParam("user") String user, SettingRepository repository,
            Audit audit) {
        terms.values().forEach((grade, value) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=terms-" + grade + " value=" + repository.termsFor(sheet.id(), grade)));
    }
}
