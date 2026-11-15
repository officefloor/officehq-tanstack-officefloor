package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Names the user asked as the person's approver (none when blank), replacing any before. */
public class SetApprover {
    public void set(@Val ApproverRequest request, @Val Person person, PersonRepository people) {
        person.setApprover(request.user().isEmpty() ? null : request.user());
        people.updateApprover(person);
    }
}
