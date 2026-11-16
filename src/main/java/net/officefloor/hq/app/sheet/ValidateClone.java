package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the id to clone the sheet to, and who clones it. */
public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneRequest> body) {
        body.set(new CloneRequest(request == null || request.to() == null ? "" : request.to().trim(),
                request == null || request.by() == null ? "" : request.by().trim()));
    }
}
