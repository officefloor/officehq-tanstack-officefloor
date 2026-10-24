package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the id of the sheet to clone this one to. */
public class ValidateClone {

    public record CloneRequest(String toSheet) {
    }

    public void validate(@RequestBody(required = false) CloneRequest request, Out<CloneInput> body)
            throws InvalidCloneException {
        String to = request == null || request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) {
            throw new InvalidCloneException("Enter the id of the sheet to clone to.");
        }
        if (to.length() > 200) {
            throw new InvalidCloneException("A sheet id is at most 200 characters.");
        }
        body.set(new CloneInput(to));
    }
}
