package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonRetired {
    public void record(@Val Person person, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " from=" + person.getRetiredFrom());
    }
}
