package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Gives the person the approver, replacing any they had. */
public class ApplyPersonApprover {
    public void apply(@Val Person person, @Val Approver approver, PersonRepository people) {
        people.saveApprover(person.sheetId(), person.id(), approver.user().isEmpty() ? null : approver.user());
    }
}
