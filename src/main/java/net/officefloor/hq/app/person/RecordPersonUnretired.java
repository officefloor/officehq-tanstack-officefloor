package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonUnretired {
    public void record(@Val Person person, @Val UnretiredOn unretired, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " on=" + unretired.on());
    }
}
