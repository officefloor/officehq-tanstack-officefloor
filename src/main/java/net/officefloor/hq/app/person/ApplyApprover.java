package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Makes the user the person's approver, replacing any they had. */
public class ApplyApprover {
    public void apply(@Val Person person, @Val ApproverChange change, PersonRepository people) {
        people.approver(person.sheetId(), person.id(), change.user());
    }
}
