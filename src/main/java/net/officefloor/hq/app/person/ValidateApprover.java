package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the approver form: a user name (no spaces; blank clears the approver). */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverInput> approver) throws BadRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("the approver must not contain spaces");
        }
        if (user.length() > 100) {
            throw new BadRequestException("the approver must be at most 100 characters");
        }
        approver.set(new ApproverInput(user));
    }
}
