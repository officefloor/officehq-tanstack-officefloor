package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the unretire, once it is saved. */
public class AuditPersonUnretired {
    public void audit(@Val Sheet sheet, @Val Person person,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + person.getName() + " on=" + sheet.getToday());
    }
}
