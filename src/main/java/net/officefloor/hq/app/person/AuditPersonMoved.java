package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the move, once it is saved. */
public class AuditPersonMoved {
    public void audit(@Val Person person, @Val PersonMoved moved,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " position=" + moved.position());
    }
}
