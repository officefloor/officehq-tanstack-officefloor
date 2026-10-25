package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonEdited {
    public void record(@Val Person person, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " grade=" + person.getGrade());
    }
}
