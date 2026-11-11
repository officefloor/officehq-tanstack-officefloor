package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverInput> body)
            throws InvalidPersonException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidPersonException("An approver is a user name");
        }
        body.set(new ApproverInput(user));
    }
}
