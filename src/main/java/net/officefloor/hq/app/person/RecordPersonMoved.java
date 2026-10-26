package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonMoved {
    public void record(@Val Person person, @Val PersonMoved moved, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " position=" + moved.position());
    }
}
