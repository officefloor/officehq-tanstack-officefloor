package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** PERSON_ADDED, as the person form records it, when the quick-add created the person. */
public class RecordQuickAddPerson {
    public void record(@Val QuickAddPerson resolved, @Val ChangedBy by, Audit audit) {
        if (!resolved.added()) {
            return;
        }
        Person person = resolved.person();
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " grade=" + person.getGrade());
    }
}
