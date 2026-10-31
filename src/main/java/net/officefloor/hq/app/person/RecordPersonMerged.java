package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the merge is saved, so a refused one writes no audit line. */
public class RecordPersonMerged {
    public void record(@Val Merger merger, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + merger.into().getSheetId() + " by=" + merger.user()
                + " from=" + merger.from().getName() + " into=" + merger.into().getName());
    }
}
