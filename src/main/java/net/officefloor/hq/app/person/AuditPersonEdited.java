package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the edited person once every rule has passed and it is saved. */
public class AuditPersonEdited {
    public void audit(@Val Person person, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + user
                + " name=" + person.getName() + " grade=" + person.getGrade());
    }
}
