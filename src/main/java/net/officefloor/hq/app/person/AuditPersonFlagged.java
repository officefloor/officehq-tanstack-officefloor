package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the flag, once it is saved. */
public class AuditPersonFlagged {
    public void audit(@Val Person person, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user) + " name="
                + person.getName() + " reason=" + person.getFlagReason() + " until=" + person.getFlagUntil());
    }
}
