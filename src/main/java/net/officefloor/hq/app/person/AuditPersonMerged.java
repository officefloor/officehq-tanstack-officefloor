package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the merge. Runs after it is saved. */
public class AuditPersonMerged {
    public void audit(@Val Sheet sheet, @Val Merge merge,
            HttpServletRequest request, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " from=" + merge.from().getName() + " into=" + merge.into().getName());
    }
}
