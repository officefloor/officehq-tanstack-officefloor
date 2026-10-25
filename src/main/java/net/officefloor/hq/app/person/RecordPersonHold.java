package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonHold {
    public void record(@Val Person person, @Val HoldSpan hold, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_HELD sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " from=" + hold.from());
    }
}
