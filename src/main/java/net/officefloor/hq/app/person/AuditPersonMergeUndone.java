package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the merge undone, once it is saved. */
public class AuditPersonMergeUndone {
    public void audit(@Val PersonMergeUndone undone, @RequestHeader(name = "X-User", required = false) String user,
            Audit audit) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + undone.from().getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + undone.restored().getName() + " from=" + undone.from().getName());
    }
}
