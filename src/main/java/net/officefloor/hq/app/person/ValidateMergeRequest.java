package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a merge that names no person to merge into. */
public class ValidateMergeRequest {
    public void validate(@RequestBody @Valid MergeRequest request, Out<MergeRequest> merge)
            throws InvalidPersonException {
        if (request.into() == null) {
            throw new InvalidPersonException("Choose the person to merge into.");
        }
        merge.set(request);
    }
}
