package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a review assignment by anyone but a steward, or to a blank user. */
public class ValidateAssignReview {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody AssignReviewRequest request,
            Out<AssignReviewInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can assign a review");
        }
        String assignee = request.assignee() == null ? "" : request.assignee().trim();
        if (assignee.isEmpty()) {
            throw new InvalidSheetException("A review must be assigned to a user");
        }
        body.set(new AssignReviewInput(user, assignee));
    }
}
