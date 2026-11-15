package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the approver: its user name trimmed (blank: none). */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverRequest> body)
            throws InvalidPersonException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.contains(" ")) {
            throw new InvalidPersonException("Not a user name: " + user);
        }
        body.set(new ApproverRequest(user));
    }
}
