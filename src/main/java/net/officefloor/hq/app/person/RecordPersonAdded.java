package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_ADDED to the audit file once the person is stored. */
public class RecordPersonAdded {
    public void record(@Val Person person, @Val PersonValues values, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.sheetId() + " by=" + values.by() + " name=" + person.name()
                + " grade=" + person.grade());
    }
}
