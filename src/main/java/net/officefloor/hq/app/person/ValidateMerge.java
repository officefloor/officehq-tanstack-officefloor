package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a merge names the person merged into and the signed-in user. */
public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeRequest> body) throws InvalidPersonException {
        if (request == null || request.intoId() == null) {
            throw new InvalidPersonException("A merge needs the person merged into");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user merging is required");
        }
        body.set(new MergeRequest(request.intoId(), by));
    }
}
