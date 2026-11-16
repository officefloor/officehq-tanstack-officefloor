package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the merge just undone (PERSON_MERGE_UNDONE name=<restored> from=<merged into>). */
public class RecordPersonMergeUndone {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val MergeUndone undone, Audit audit) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + undone.from().getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + undone.restored().getName() + " from=" + undone.from().getName());
    }
}
