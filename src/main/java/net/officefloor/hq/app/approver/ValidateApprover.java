package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateApprover {
    // The approver's user name must not be blank.
    public void validate(@RequestBody ApproverRequest body, Out<Approver> approver) throws InvalidPersonException {
        String user = body == null || body.user() == null ? "" : body.user().trim();
        if (user.isEmpty()) {
            throw new InvalidPersonException("Give the approver's user name.");
        }
        approver.set(new Approver(0, user));
    }
}
