package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the merge body once, refusing one that names no person. */
public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeRequest> body) throws InvalidPersonException {
        if (request == null || request.into() == null) {
            throw new InvalidPersonException("A merge names the person merged into");
        }
        body.set(request);
    }
}
