package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just flagged, the reason and the date the flag is in force until. */
public class AuditPersonFlagged {
    public void audit(@Val Person person, @Val Flag flag, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " reason=" + flag.reason() + " until=" + flag.until());
    }
}
