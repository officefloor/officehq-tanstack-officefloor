package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the saved person, with the grade in force on the sheet's today. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditPersonEdited {
    public void audit(@Val Sheet sheet, @Val Person person,
            HttpServletRequest request, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " grade=" + person.gradeOn(sheet.today()));
    }
}
