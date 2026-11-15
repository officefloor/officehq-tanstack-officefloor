package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the sheet to clone to (trimmed): refused when none is named. */
public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneRequest> body)
            throws InvalidSheetCloneException {
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidSheetCloneException("No sheet to clone to");
        }
        body.set(new CloneRequest(to));
    }
}
