package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the merge just made (PERSON_MERGED). */
public class RecordPersonMerged {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Merge merge, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + merge.into().getSheetId() + " by=" + (by == null ? "" : by)
                + " from=" + merge.from().getName() + " into=" + merge.into().getName());
    }
}
