package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the clone body once, refusing one that names no sheet. */
public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneRequest> body) throws InvalidCloneException {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new InvalidCloneException("A clone names the sheet cloned to");
        }
        body.set(new CloneRequest(request.to().trim()));
    }
}
