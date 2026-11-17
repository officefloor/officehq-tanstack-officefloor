package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithApprover {
    public void respond(@Val Person person, @Val ApproverChange change, ObjectResponse<Approver> response) {
        response.send(new Approver(person.id(), change.user()));
    }
}
