package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the id of the person to merge into. */
public class ValidateMerge {

    public record MergeRequest(Long intoId) {
    }

    public void validate(@RequestBody(required = false) MergeRequest request, Out<MergeInput> body)
            throws InvalidPersonException {
        if (request == null || request.intoId() == null) {
            throw new InvalidPersonException("Choose a person to merge into.");
        }
        body.set(new MergeInput(request.intoId()));
    }
}
