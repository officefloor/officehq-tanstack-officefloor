package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records that a merge was undone: the person restored, and the person they had been merged into. Runs after it is saved. */
public class AuditPersonMergeUndone {
    public void audit(@Val Sheet sheet, @Val Merge merge, HttpServletRequest request, Audit audit) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + merge.from().getName() + " from=" + merge.into().getName());
    }
}
