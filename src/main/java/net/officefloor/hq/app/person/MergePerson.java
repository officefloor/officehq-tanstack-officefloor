package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Stores the merge (see PersonMerger#merge). */
public class MergePerson {
    public void merge(@Val PersonMerge merge, PersonMerger merger) {
        merger.merge(merge.from().sheetId(), merge.from(), merge.into());
    }
}
