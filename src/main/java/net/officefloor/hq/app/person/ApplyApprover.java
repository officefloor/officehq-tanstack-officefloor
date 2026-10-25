package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Makes the user the person's approver, replacing any they had (none when blank). */
public class ApplyApprover {
    public void apply(@Val ApproverInput input, @Val Person person, PersonRepository repository) {
        person.setApprover(input.user());
        repository.saveAndFlush(person);
    }
}
