package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, and an approver name of no
 *  more than 100 characters without spaces (blank clears it). */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before setting approvers.");
        }
        String approver = request.approver() == null ? "" : request.approver().trim();
        if (approver.length() > 100 || approver.matches(".*\\s.*")) {
            throw new InvalidPersonException("An approver is one user name, of no more than 100 characters.");
        }
        body.set(new ApproverInput(request.user().trim(), approver));
    }
}
