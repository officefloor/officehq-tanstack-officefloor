package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and an id (at most 100 characters) to clone
 *  the sheet to. */
public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneRequest> body) throws InvalidSheetException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in before cloning the sheet.");
        }
        String to = request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty() || to.length() > 100) {
            throw new InvalidSheetException("Enter the id of the sheet to clone to (at most 100 characters).");
        }
        body.set(new CloneRequest(request.user().trim(), to));
    }
}
