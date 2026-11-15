package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the person to merge into: refused when none is named. */
public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeRequest> body)
            throws InvalidPersonException {
        if (request == null || request.intoId() == null) {
            throw new InvalidPersonException("No person to merge into");
        }
        body.set(request);
    }
}
