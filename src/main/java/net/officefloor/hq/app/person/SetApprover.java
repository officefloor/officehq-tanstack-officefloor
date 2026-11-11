package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Sets the person's approver ("" clears it). */
public class SetApprover {
    public void apply(@Val ApproverInput input, @Val Person person, PersonRepository repository) {
        person.setApprover(input.user().isEmpty() ? null : input.user());
        repository.saveApprover(person);
    }
}
