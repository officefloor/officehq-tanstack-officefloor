package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Makes the user the person's approver, replacing any they had (a blank one clears it). */
public class SetApprover {
    public void set(@Val ApproverInput input, @Val Person person, PersonStore store) {
        store.approver(person, input.approver());
    }
}
