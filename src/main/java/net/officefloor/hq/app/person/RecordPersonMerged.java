package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonMerged {
    public void record(@Val Person person, @Val PersonMerged merged, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_MERGED sheet=" + person.getSheetId() + " by=" + by.name() + " from=" + merged.from()
                + " into=" + merged.into());
    }
}
