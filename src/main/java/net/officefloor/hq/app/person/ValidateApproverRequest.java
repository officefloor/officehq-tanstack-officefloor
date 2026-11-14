package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) an approver whose name has a space in it; a blank one is none. */
public class ValidateApproverRequest {
    public void validate(@RequestBody @Valid ApproverRequest request, Out<Approver> approver)
            throws InvalidPersonException {
        String user = request.user() == null ? "" : request.user().trim();
        if (user.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidPersonException("An approver is a single user name.");
        }
        approver.set(new Approver(user));
    }
}
