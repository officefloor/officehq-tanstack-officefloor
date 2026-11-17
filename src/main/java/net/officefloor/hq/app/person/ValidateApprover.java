package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the approver's user name (a single word) and the signed-in user are required. */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverChange> change) throws InvalidPersonException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty() || user.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidPersonException("The approver must be a user name");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user setting the approver is required");
        }
        change.set(new ApproverChange(user, by));
    }
}
