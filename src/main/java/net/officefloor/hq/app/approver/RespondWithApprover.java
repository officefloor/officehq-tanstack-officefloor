package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithApprover {
    public void respond(@Val Approver approver, @Val Person person, ObjectResponse<Approver> response) {
        response.send(new Approver(person.id(), approver.user()));
    }
}
