package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_MERGED from=<merged name> into=<name> to the audit file once the merge is stored, then the figures the
 *  merge moved (see Rollup). */
public class RecordPersonMerged {
    public void record(@Val PersonMerge merge, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String sheetId = merge.from().sheetId();
        audit.record("PERSON_MERGED sheet=" + sheetId + " by=" + merge.by() + " from=" + merge.from().name() + " into="
                + merge.into().name());
        rollup.record(sheetId, merge.by(), before);
    }
}
