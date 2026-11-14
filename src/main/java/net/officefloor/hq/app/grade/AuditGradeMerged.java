package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the grade merged, ahead of the edits of the people it moves. */
public class AuditGradeMerged {
    public void audit(@Val GradeMerge merge, @RequestParam("user") String user, Audit audit) {
        audit.record("GRADE_MERGED sheet=" + merge.sheetId() + " by=" + user + " from=" + merge.from() + " into="
                + merge.into());
    }
}
