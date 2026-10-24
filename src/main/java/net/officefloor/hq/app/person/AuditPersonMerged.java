package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the merge, once it is saved. */
public class AuditPersonMerged {
    public void audit(@Val PersonMerged merged, @RequestHeader(name = "X-User", required = false) String user,
            Audit audit) {
        audit.record("PERSON_MERGED sheet=" + merged.into().getSheetId() + " by=" + AuditUser.decode(user)
                + " from=" + merged.from() + " into=" + merged.into().getName());
    }
}
