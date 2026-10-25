package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the approver's user name, trimmed (blank: no approver). */
public class ValidateApprover {

    public record ApproverRequest(String user) {
    }

    public void validate(@RequestBody(required = false) ApproverRequest request, Out<ApproverInput> body) {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        body.set(new ApproverInput(user.isEmpty() ? null : user));
    }
}
