package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonFlagged {
    public void record(@Val Person person, @Val FlagInput flag, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " reason=" + flag.reason() + " until=" + flag.until());
    }
}
