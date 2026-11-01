package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the body: a blank approver clears the person's approver. */
public class ValidateApprover {
    public void validate(@RequestBody ApproverRequest request, Out<ApproverChange> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request != null ? request.user() : null;
        if (user == null || user.isBlank()) throw new InvalidSheetRequestException("Who sets the approver is needed");
        String approver = request.approver() == null ? "" : request.approver().trim();
        if (approver.contains(" ")) throw new InvalidSheetRequestException("Not a user name: " + approver);
        body.set(new ApproverChange(approver.isEmpty() ? null : approver, user));
        actor.set(new Actor(user));
    }
}
