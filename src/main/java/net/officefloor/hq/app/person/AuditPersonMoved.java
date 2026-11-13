package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just moved and their new position in the list, counting from 1. */
public class AuditPersonMoved {
    public void audit(@Val PersonMoved moved, @RequestParam("user") String user, Audit audit) {
        Person person = moved.person();
        audit.record("PERSON_MOVED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " position=" + moved.position());
    }
}
