package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the edited person, once it is saved, with the grade in force on the sheet's today, and their terms in force
 *  on it when the edit changed any of theirs (empty when cleared), and likewise their contact. */
public class AuditPersonEdited {
    public void audit(@Val Person person, @Val Sheet sheet, PersonGradeRepository grades,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record(PersonRules.editedLine(person, grades.gradesOf(person).on(sheet.getToday()),
                person.termsOn(sheet.getToday()),
                AuditUser.decode(user)));
    }
}
