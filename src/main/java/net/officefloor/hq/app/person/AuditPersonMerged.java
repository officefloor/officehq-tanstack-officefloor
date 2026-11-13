package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just merged and the person they were merged into. */
public class AuditPersonMerged {
    public void audit(@Val Merge merge, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + merge.from().sheetId() + " by=" + user + " from=" + merge.from().name()
                + " into=" + merge.into().name());
    }
}
