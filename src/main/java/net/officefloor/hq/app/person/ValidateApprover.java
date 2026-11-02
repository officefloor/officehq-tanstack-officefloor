package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the approver's user name, trimmed; a blank one clears the approver. */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<Approver> approver) throws InvalidPersonException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.contains(" ")) {
            throw new InvalidPersonException("An approver is a single user name");
        }
        approver.set(new Approver(user));
    }
}
